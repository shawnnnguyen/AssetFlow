package com.project3.AssetFlow.streaming.handler;

import com.project3.AssetFlow.streaming.events.FinnhubConnectedEvent;
import com.project3.AssetFlow.streaming.events.FinnhubDisconnectedEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
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

    @EventListener
    public void onDisconnected(FinnhubDisconnectedEvent event) {
        log.warn("Disconnected event received! Initiating reconnect sequence...");
        scheduleReconnect();
    }

    @EventListener
    public void onConnected(FinnhubConnectedEvent event) {
        log.info("Connected event received! Resetting backoff delay.");
        resetBackoff();
    }

    public void scheduleReconnect() {
        log.info("Scheduling WebSocket reconnect in {} ms", currentRetryDelayMs);

        scheduler.schedule(() -> {
            try {
                log.info("Attempting to reconnect to Finnhub...");
                connectionManager.start();
            } catch (Exception e) {
                log.error("Reconnect attempt failed, retrying in {} ms", currentRetryDelayMs, e);
                currentRetryDelayMs = Math.min(currentRetryDelayMs * 2, MAX_RETRY_DELAY_MS);
                scheduleReconnect();
            }
        }, currentRetryDelayMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    public void resetBackoff() {
        this.currentRetryDelayMs = 1000;
    }
}
