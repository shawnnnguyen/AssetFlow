package com.project3.AssetFlow.cash_transaction.dto;

import com.project3.AssetFlow.cash_transaction.CashTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record CashTransactionResponse(
        Long transactionId,
        Long portfolioId,
        CashTransactionType type,
        BigDecimal amount,
        Instant executedAt
) {
}
