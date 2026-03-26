package com.project3.AssetFlow.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioValueDTO(
    Long portfolioId,
    String currency,

    BigDecimal totalMarketValue,
    BigDecimal totalAvgCost,
    BigDecimal totalValueChange,
    BigDecimal totalPercentageChange,
    BigDecimal cashBalance,
    BigDecimal totalNetWorth,

    List<AssetPerformanceDTO> assetsBreakdown
) {
    public PortfolioValueDTO {
        assetsBreakdown = List.copyOf(assetsBreakdown);
    }
}