package com.project3.AssetFlow.streaming.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.finnhub")
public record SubscribeData(
        List<String> tickers
) {
}
