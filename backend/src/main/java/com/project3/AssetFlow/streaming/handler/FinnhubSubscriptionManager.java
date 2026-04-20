package com.project3.AssetFlow.streaming.handler;

import com.project3.AssetFlow.streaming.dto.FinnhubRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

@Component
public class FinnhubSubscriptionManager {

    private static final Logger log = LoggerFactory.getLogger(FinnhubSubscriptionManager.class);

    private final ObjectMapper mapper;
    private volatile WebSocketSession activeSession;

    public FinnhubSubscriptionManager(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void setSession(WebSocketSession session) {
        this.activeSession = session;
    }

    public void clearSession() {
        this.activeSession = null;
    }

    public void subscribeToTicker(String ticker) {
        sendRequest(new FinnhubRequest("subscribe", ticker));
    }

    public void unsubscribeFromTicker(String ticker) {
        sendRequest(new FinnhubRequest("unsubscribe", ticker));
    }

    private void sendRequest(FinnhubRequest request) {
        if(activeSession != null && activeSession.isOpen()) {
            try {
                String jsonMessage = mapper.writeValueAsString(request);
                activeSession.sendMessage(new TextMessage(jsonMessage));
            }
            catch (Exception e) {
                log.error("Error subscribing to ticker: {}", request.symbol(), e);
            }
        }
        else {
            log.warn("Cannot send request for {}. WebSocket session is not open.", request.symbol());
        }
    }
}
