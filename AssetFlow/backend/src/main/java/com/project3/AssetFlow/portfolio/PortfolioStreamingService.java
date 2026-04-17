package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.holdings.Holding;
import com.project3.AssetFlow.holdings.HoldingRepository;
import com.project3.AssetFlow.market.MarketDataService;
import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import com.project3.AssetFlow.portfolio.dto.PortfolioPerformanceResponse;
import com.project3.AssetFlow.streaming.events.PriceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final Set<Long> pendingPortfolioIds = ConcurrentHashMap.newKeySet();

    @Async
    @EventListener
    @Transactional(readOnly = true)
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

    @Scheduled(fixedRate = 1000)
    public void processPendingPortfolios() {
        if(pendingPortfolioIds.isEmpty()) return;

        Set<Long> portfolioToProcess = Set.copyOf(pendingPortfolioIds);

        pendingPortfolioIds.removeAll(portfolioToProcess);

        if(portfolioToProcess.isEmpty()) return;

        Map<Long, BigDecimal> livePrices = marketDataService.getAllTrackedStocks()
                .stream()
                .collect(Collectors.toMap(
                        TrackedStocksDTO::assetId,
                        TrackedStocksDTO::latestPrice,
                        (existing, replacement) -> existing
                ));

        if (livePrices.isEmpty()) return;

        for(Long portfolioId : portfolioToProcess) {
            try {
                Portfolio portfolio = portfolioRepository.findById(portfolioId).orElseThrow();

                List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);

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
