package com.project3.AssetFlow.transaction.dto;

import com.project3.AssetFlow.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long transactionId,
        Long portfolioId,
        Long assetId,
        BigDecimal quantity,
        BigDecimal pricePerUnit,
        Instant executedAt,
        TransactionType type
) {
}
