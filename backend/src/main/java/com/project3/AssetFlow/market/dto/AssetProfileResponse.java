package com.project3.AssetFlow.market.dto;

public record AssetProfileResponse(
        String ticker,
        String name,
        String country,
        String currency,
        String industry
) {}
