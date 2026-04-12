package com.project3.AssetFlow.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioResponse(
        Long id,
        String name,
        String currency,
        BigDecimal cashBalance
) {}
