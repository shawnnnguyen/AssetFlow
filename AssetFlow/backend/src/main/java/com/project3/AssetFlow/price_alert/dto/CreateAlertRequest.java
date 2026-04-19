package com.project3.AssetFlow.price_alert.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateAlertRequest(

        @NotNull String ticker,

        @NotNull @Positive
        BigDecimal targetPrice
) {
}
