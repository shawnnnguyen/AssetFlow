package com.project3.AssetFlow.cash_transaction.dto;

import com.project3.AssetFlow.cash_transaction.CashTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashTransactionRequest(

        @NotNull
        Long userId,

        @NotNull
        Long portfolioId,

        @NotNull
        CashTransactionType type,

        @NotNull @Positive
        BigDecimal amount
) {
}
