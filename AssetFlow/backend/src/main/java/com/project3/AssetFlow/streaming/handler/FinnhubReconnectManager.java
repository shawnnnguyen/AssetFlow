package com.project3.AssetFlow.streaming.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketConnectionManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class FinnhubReconnectManager {

    private static final Logger log = LoggerFactory.getLogger(FinnhubReconnectManager.class);

    private final WebSocketConnectionManager connectionManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private int currentRetryDelayMs = 1000;
    private static final int MAX_RETRY_DELAY_MS = 60000;

    public FinnhubReconnectManager(WebSocketConnectionManager finnhubWebSocketClient) {
        this.connectionManager = finnhubWebSocketClient;
    }

    public void scheduleReconnect() {
        log.info("Scheduling WebSocket reconnect in {} ms", currentRetryDelayMs);

        scheduler.schedule(() -> {
            log.info("Attempting to reconnect to Finnhub...");

            connectionManager.start();

            currentRetryDelayMs = Math.min(currentRetryDelayMs * 2, MAX_RETRY_DELAY_MS);

        }, currentRetryDelayMs, TimeUnit.MILLISECONDS);
    }

    public void resetBackoff() {
        this.currentRetryDelayMs = 1000;
    }
}
