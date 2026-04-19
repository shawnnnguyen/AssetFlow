package com.project3.AssetFlow.price_alert.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateAlertRequest(

        @NotNull @Positive
        BigDecimal newTargetPrice) {
}
