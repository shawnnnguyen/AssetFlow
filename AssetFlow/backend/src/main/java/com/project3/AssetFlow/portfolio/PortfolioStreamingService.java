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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void handleAssetPriceUpdate(PriceUpdateEvent event) {

     try {
         Map<Long, BigDecimal> livePrices = marketDataService.getAllTrackedStocks()
                 .stream()
                 .collect(Collectors.toMap(
                         TrackedStocksDTO::assetId,
                         TrackedStocksDTO::latestPrice,
                         (existing, replacement) -> existing
                 ));

        List<Portfolio> affectedPortfolios = portfolioRepository.findPortfoliosByAssetIdWithUser(event.assetId());

         if (livePrices.isEmpty()) {
             return;
         }

         for(Portfolio portfolio : affectedPortfolios) {
             Long portfolioId = portfolio.getId();
             Long userId = portfolio.getUser().getId();

             List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);

             PortfolioPerformanceResponse response = portfolioService.calculatePortfolioPerformance(
                     portfolio,
                     holdings,
                     livePrices);

             messagingTemplate.convertAndSendToUser(
                     String.valueOf(userId),
                     "/queue/portfolio/" + portfolioId,
                     response
             );
             }
         } catch (Exception e) {
             log.error("Error calculating portfolio performance", e);
         }
     }
}
