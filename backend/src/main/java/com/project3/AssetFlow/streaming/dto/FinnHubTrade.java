package com.project3.AssetFlow.streaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnHubTrade(
        @JsonProperty("s")
        String ticker,

        @JsonProperty("p")
        BigDecimal price,

        @JsonProperty("t")
        Long timestamp
) {}
