package com.project3.AssetFlow.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioResponse(
        Long id,
        String name,
        String currencyCode,
        BigDecimal cashBalance
) {}
