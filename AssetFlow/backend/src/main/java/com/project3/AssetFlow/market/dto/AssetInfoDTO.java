package com.project3.AssetFlow.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AssetInfoDTO(
        String ticker,
        String country,

        @JsonProperty("currency")
        String currencyCode,
        String name,

        @JsonProperty("finnhubIndustry")
        String industry
) {}
