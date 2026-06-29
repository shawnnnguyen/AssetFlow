package com.project3.AssetFlow.transaction.dto;

import com.project3.AssetFlow.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID portfolioId,
        UUID assetId,
        BigDecimal quantity,
        BigDecimal pricePerUnit,
        String currencyCode,
        Instant executedAt,
        TransactionType type
) {
}
