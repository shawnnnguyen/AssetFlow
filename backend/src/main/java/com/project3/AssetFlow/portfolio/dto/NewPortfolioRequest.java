package com.project3.AssetFlow.portfolio.dto;

import jakarta.validation.constraints.NotBlank;

public record NewPortfolioRequest(
        @NotBlank
        String name,

        @NotBlank
        String currencyCode
) {
}
