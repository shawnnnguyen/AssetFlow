package com.project3.AssetFlow.streaming.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinnhubRequest(
    @JsonProperty("type")
    String type,

    @JsonProperty("symbol")
    String symbol
) {}
