package com.project3.AssetFlow.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioResponse(
        UUID id,
        String name,
        String currencyCode,
        BigDecimal cashBalance,
        String status,
        Instant createdAt
) {}
