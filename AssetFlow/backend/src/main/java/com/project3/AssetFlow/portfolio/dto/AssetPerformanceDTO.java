package com.project3.AssetFlow.portfolio.dto;

import java.math.BigDecimal;

public record AssetPerformanceDTO(
        String ticker,
        BigDecimal quantity,
        BigDecimal currentPrice,
        BigDecimal avgCost,
        BigDecimal percentageChange,
        BigDecimal valueChange
) {}