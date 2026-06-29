package com.project3.AssetFlow.portfolio.dto;

import com.project3.AssetFlow.portfolio.PortfolioStatusType;

import java.math.BigDecimal;
import java.util.UUID;

public record NewPortfolioResponse(
        UUID id,
        String name,
        PortfolioStatusType status,
        String currencyCode,
        BigDecimal cashBalance
) {
}
