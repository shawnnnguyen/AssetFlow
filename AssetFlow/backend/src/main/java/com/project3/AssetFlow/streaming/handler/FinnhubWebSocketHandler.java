package com.project3.AssetFlow.streaming.handler;

import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.market.AssetRepository;
import com.project3.AssetFlow.market.MarketDataService;
import com.project3.AssetFlow.streaming.dto.FinnhubResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FinnhubWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FinnhubWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final AssetRepository assetRepository;
    private final MarketDataService marketDataService;
    private final ApplicationEventPublisher eventPublisher;
    private final FinnhubSubscriptionManager subscriptionManager;

    public void subscribeToTicker(String ticker) {
       subscriptionManager.subscribeToTicker(ticker);
    }

    public void unsubscribeFromTicker(String ticker) {
        subscriptionManager.unsubscribeFromTicker(ticker);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established");
        subscriptionManager.setSession(session);
        eventPublisher.publishEvent(new FinnhubConnectedEvent(this));

        List<Asset> trackedAssets = assetRepository.findAll();
        trackedAssets.forEach(asset -> subscribeToTicker(asset.getTicker()));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        FinnhubResponse response = objectMapper.readValue(payload, FinnhubResponse.class);

        if("trade".equals(response.type()) && response.data() != null) {
            marketDataService.processTrades(response.data());
            log.debug("Received message: {}", response);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error occurred", exception);
        subscriptionManager.clearSession();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("WebSocket connection closed. Status: {}", status);

        subscriptionManager.clearSession();
        eventPublisher.publishEvent(new FinnhubDisconnectedEvent(this));
    }
}