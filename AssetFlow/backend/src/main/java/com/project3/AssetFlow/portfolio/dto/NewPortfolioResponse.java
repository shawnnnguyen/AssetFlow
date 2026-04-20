package com.project3.AssetFlow.portfolio.dto;

import com.project3.AssetFlow.portfolio.PortfolioStatusType;

import java.math.BigDecimal;

public record NewPortfolioResponse(
        Long id,
        Long userId,
        String name,
        PortfolioStatusType status,
        String currencyCode,
        BigDecimal cashBalance
) {
}
