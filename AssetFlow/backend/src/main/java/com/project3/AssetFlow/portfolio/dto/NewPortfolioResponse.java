package com.project3.AssetFlow.portfolio.dto;

import java.math.BigDecimal;

public record NewPortfolioResponse(
        Long id,
        String name,
        String currency,
        BigDecimal cashBalance
) {}
