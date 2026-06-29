package com.project3.AssetFlow.cash_transaction.dto;

import com.project3.AssetFlow.cash_transaction.CashTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CashTransactionRequest(
        @NotNull
        UUID portfolioId,

        @NotNull
        CashTransactionType type,

        @NotNull @Positive
        BigDecimal amount
) {
}
