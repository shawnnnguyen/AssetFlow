package com.project3.AssetFlow.streaming.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record FinnHubTrade(
        @JsonProperty("s") String ticker,
        @JsonProperty("p") BigDecimal price,
        @JsonProperty("t") long timestamp,
        @JsonProperty("v") BigDecimal volume
) {
}
