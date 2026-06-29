package com.project3.AssetFlow.transaction.dto;

import com.project3.AssetFlow.transaction.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID assetId,
        @NotNull UUID portfolioId,
        @NotNull Instant executedAt,
        @NotNull @Positive BigDecimal quantity,
        @NotNull TransactionType type
) {
}
