package com.project3.AssetFlow.holdings;

import com.project3.AssetFlow.holdings.dto.HoldingResponse;
import com.project3.AssetFlow.holdings.dto.HoldingPerformance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingRepository holdingRepository;

    public List<HoldingResponse> getHoldingsByPortfolioId(Long portfolioId) {

        return holdingRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(holding -> new HoldingResponse(
                        holding.getId(),
                        holding.getAsset().getId(),
                        holding.getPortfolio().getId(),
                        holding.getQuantity(),
                        holding.getAvgCost(),
                        holding.getAvgCost().multiply(holding.getQuantity())
                ))
                .toList();
    }

    public List<HoldingResponse> getHoldingsByTicker(String ticker, Long portfolioId) {
        return holdingRepository.findByTickerAndPortfolioId(ticker, portfolioId)
                .stream()
                .map(holding -> new HoldingResponse(
                        holding.getId(),
                        holding.getAsset().getId(),
                        holding.getPortfolio().getId(),
                        holding.getQuantity(),
                        holding.getAvgCost(),
                        holding.getAvgCost().multiply(holding.getQuantity())
                ))
                .toList();
    }

    public HoldingPerformance calculateHoldingPerformance(Holding holding, BigDecimal latestPrice) {

        BigDecimal totalCost = holding.getQuantity().multiply(holding.getAvgCost());
        BigDecimal currentTotalValue = holding.getQuantity().multiply(latestPrice);

        BigDecimal absoluteChange = currentTotalValue.subtract(totalCost);

        BigDecimal percentageChange = BigDecimal.ZERO;

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = absoluteChange
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new HoldingPerformance(
                holding.getId(),
                holding.getAsset().getId(),
                latestPrice,
                currentTotalValue,
                absoluteChange,
                percentageChange
        );
    }
}
