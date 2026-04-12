package com.project3.AssetFlow.portfolio.dto;

import com.project3.AssetFlow.holdings.dto.HoldingPerformance;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioPerformanceResponse(
        Long portfolioId,
        BigDecimal totalInvestedValue,
        BigDecimal portfolioValue,
        PerformanceMetrics metrics,
        List<HoldingPerformance> holdings
) {
}
