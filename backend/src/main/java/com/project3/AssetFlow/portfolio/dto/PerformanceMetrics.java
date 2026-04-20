package com.project3.AssetFlow.portfolio.dto;

import java.math.BigDecimal;

public record PerformanceMetrics(
        BigDecimal absoluteChange,
        BigDecimal percentageChange
) {
}
