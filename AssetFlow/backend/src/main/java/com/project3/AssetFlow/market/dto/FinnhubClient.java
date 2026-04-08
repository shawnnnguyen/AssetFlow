package com.project3.AssetFlow.market.dto;

import com.project3.AssetFlow.market.Asset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinnhubClient {

    private final RestClient restClient;

    public FinnhubClient(RestClient.Builder restClientBuilder,
                         @Value("${app.finnhub.http-method.base-url}") String baseUrl,
                         @Value("${app.finnhub.api-key}") String apiKey) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("X-Finnhub-Token", apiKey)
                .build();
    }

    public AssetInfoDTO getCompanyProfile (String ticker) {
        AssetInfoDTO result = restClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                .path("/stock/profile2")
                                                .queryParam("symbol", ticker)
                                                .build())
                                        .retrieve()
                                        .body(AssetInfoDTO.class);

        return (result != null && result.name() != null) ? result : null;
    }
}
