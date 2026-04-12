package com.project3.AssetFlow.streaming.config;

import com.project3.AssetFlow.streaming.handler.FinnhubWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
@EnableAsync
public class FinnhubWebSocketConfig {

    @Value("${app.finnhub.api-key}")
    private String apiKey;

    @Value("${app.finnhub.websocket.base-url}")
    private String baseUrl;

    @Bean
    public WebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }

    @Bean
    public WebSocketConnectionManager finnhubWebSocketClient (WebSocketClient client, FinnhubWebSocketHandler handler) {
        String uri = baseUrl + "?token=" + apiKey;

        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, handler, uri);
        manager.setAutoStartup(true);
        return manager;
    }
}
