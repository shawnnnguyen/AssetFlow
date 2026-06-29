package com.project3.AssetFlow.portfolio.dto;

import com.project3.AssetFlow.holdings.dto.HoldingPerformance;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PortfolioPerformanceResponse(
        UUID portfolioId,
        BigDecimal totalInvestedValue,
        BigDecimal portfolioValue,
        BigDecimal cashBalance,
        PerformanceMetrics metrics,
        List<HoldingPerformance> holdings
) {
}
