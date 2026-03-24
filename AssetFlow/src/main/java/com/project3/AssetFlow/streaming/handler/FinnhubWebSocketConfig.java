package com.project3.AssetFlow.streaming.handler;

import com.project3.AssetFlow.streaming.dto.SubscribeData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
@EnableConfigurationProperties(SubscribeData.class)
public class FinnhubWebSocketConfig {

    @Value("${app.finnhub.api-key}")
    private String apiKey;

    @Value("${app.finnhub.base-url}")
    private String baseUrl;

    @Bean
    public WebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }

    @Bean
    public WebSocketConnectionManager finnhubClient (WebSocketClient client, FinnhubWebSocketHandler handler) {
        String uri = baseUrl + "?token=" + apiKey;

        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler, uri);
        manager.setAutoStartup(true);
        return manager;
    }
}
