package com.project3.AssetFlow.streaming.handler;

import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.market.AssetRepository;
import com.project3.AssetFlow.market.MarketDataService;
import com.project3.AssetFlow.streaming.dto.FinnhubRequest;
import com.project3.AssetFlow.streaming.dto.FinnhubResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class FinnhubWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FinnhubWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final AssetRepository assetRepository;
    private final MarketDataService marketDataService;
    @Lazy private final FinnhubReconnectManager reconnectManager;
    @Lazy private WebSocketSession activeSession;

    public FinnhubWebSocketHandler(ObjectMapper objectMapper,
                                   AssetRepository assetRepository,
                                   @Lazy MarketDataService marketDataService,
                                   @Lazy FinnhubReconnectManager reconnectManager) {
        this.objectMapper = objectMapper;
        this.assetRepository = assetRepository;
        this.marketDataService = marketDataService;
        this.reconnectManager = reconnectManager;

    }

    public void subscribeToTicker(String ticker) {
        if(activeSession != null && activeSession.isOpen()) {
            try {
                FinnhubRequest request = new FinnhubRequest("subscribe", ticker);
                String jsonMessage = objectMapper.writeValueAsString(request);
                activeSession.sendMessage(new TextMessage(jsonMessage));
            }
            catch (Exception e) {
                log.error("Error subscribing to ticker: {}", ticker, e);
            }
        }
    }

    public void unsubscribeFromTicker(String ticker) {
        if(activeSession != null && activeSession.isOpen()) {
            try {
                FinnhubRequest request = new FinnhubRequest("unsubscribe", ticker);
                String jsonMessage = objectMapper.writeValueAsString(request);
                activeSession.sendMessage(new TextMessage(jsonMessage));
            }
            catch (Exception e) {
                log.error("Error unsubscribing to ticker: {}", ticker, e);
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established");
        this.activeSession = session;

        List<Asset> trackedAssets = assetRepository.findAll();
        trackedAssets.forEach(asset -> subscribeToTicker(asset.getTicker()));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        FinnhubResponse response = objectMapper.readValue(payload, FinnhubResponse.class);

        if("trade".equals(response.type()) && response.data() != null) {
            marketDataService.processTrades(response.data());
            System.out.println("Received message: " + payload);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error occurred", exception);
        reconnectManager.scheduleReconnect();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("WebSocket connection closed. Status: {}", status);
        this.activeSession = null;
        reconnectManager.scheduleReconnect();
    }
}