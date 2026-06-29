package com.project3.AssetFlow.cash_transaction.dto;

import com.project3.AssetFlow.cash_transaction.CashTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashTransactionResponse(
        UUID transactionId,
        UUID portfolioId,
        CashTransactionType type,
        BigDecimal amount,
        Instant executedAt
) {
}
