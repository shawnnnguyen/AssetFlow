package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.holdings.Holding;
import com.project3.AssetFlow.holdings.HoldingRepository;
import com.project3.AssetFlow.market.MarketDataService;
import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import com.project3.AssetFlow.portfolio.dto.PortfolioPerformanceResponse;
import com.project3.AssetFlow.streaming.events.PortfolioCashChangedEvent;
import com.project3.AssetFlow.streaming.events.PriceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioStreamingService {

    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final SimpMessagingTemplate messagingTemplate;

    private final Set<UUID> pendingPortfolioIds = ConcurrentHashMap.newKeySet();

    @Async("portfolioCalcExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handleAssetPriceUpdate(PriceUpdateEvent event) {

        try {

        List<Portfolio> affectedPortfolios = portfolioRepository.findPortfoliosByAssetIdWithUser(event.assetId());

         for(Portfolio portfolio : affectedPortfolios) {
             pendingPortfolioIds.add(portfolio.getId());
            }
         } catch (Exception e) {
             log.error("Error calculating portfolio performance", e);
         }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCashBalanceChanged(PortfolioCashChangedEvent event) {
        pendingPortfolioIds.add(event.portfolioId());
    }

    @Scheduled(fixedRateString = "${app.portfolio.performance-recalc-interval-ms:1000}")
    public void processPendingPortfolios() {
        if(pendingPortfolioIds.isEmpty()) return;

        Set<UUID> portfolioToProcess = Set.copyOf(pendingPortfolioIds);

        pendingPortfolioIds.removeAll(portfolioToProcess);

        if(portfolioToProcess.isEmpty()) return;

        Map<UUID, BigDecimal> livePrices = marketDataService.getAllTrackedStocks()
                .stream()
                .filter(s -> s.latestPrice() != null)
                .collect(Collectors.toMap(
                        TrackedStocksDTO::assetId,
                        TrackedStocksDTO::latestPrice,
                        (existing, replacement) -> existing
                ));

        if (livePrices.isEmpty()) return;

        for(UUID portfolioId : portfolioToProcess) {
            try {
                Portfolio portfolio = portfolioRepository.findById(portfolioId).orElseThrow();

                List<Holding> holdings = holdingRepository.findByPortfolioIdWithDetails(portfolioId);

                PortfolioPerformanceResponse response = portfolioService.calculatePortfolioPerformance(
                        portfolio,
                        holdings,
                        livePrices);

                messagingTemplate.convertAndSendToUser(
                        String.valueOf(portfolio.getUser().getId()),
                        "/queue/portfolio/" + portfolioId,
                        response);

            } catch (Exception e) {
                log.error("Error calculating portfolio performance", e);
            }
        }
    }
}
