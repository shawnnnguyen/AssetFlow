package com.project3.AssetFlow.streaming.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinnhubResponse(
        @JsonProperty("data")
        FinnHubTrade[] data,

        @JsonProperty("type")
        String type
) {}
