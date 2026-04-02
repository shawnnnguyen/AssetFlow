package com.project3.AssetFlow.market.dto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinnhubClient {

    private RestClient restClient;

    public FinnhubClient(RestClient.Builder builder,
                         @Value("${app.finnhub.http-method.base-url}") String baseUrl,
                         @Value("${app.finnhub.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Finnhub-Token", apiKey)
                .build();
    }

    public AssetInfoDTO getCompanyProfile (String ticker) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stock/profile2")
                        .queryParam("symbol", ticker)
                        .build())
                .retrieve()
                .body(AssetInfoDTO.class);
    }
}
