package com.project3.AssetFlow.streaming.handler;

import com.project3.AssetFlow.market.MarketDataService;
import com.project3.AssetFlow.streaming.dto.FinnhubRequest;
import com.project3.AssetFlow.streaming.dto.FinnhubResponse;
import com.project3.AssetFlow.streaming.dto.SubscribeData;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class FinnhubWebSocketHandler extends TextWebSocketHandler {

    private ObjectMapper objectMapper;

    private SubscribeData subscribeData;

    private MarketDataService marketDataService;

    public FinnhubWebSocketHandler(ObjectMapper objectMapper, SubscribeData subscribeData) {
        this.objectMapper = objectMapper;
        this.subscribeData = subscribeData;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        List<String> tickers = subscribeData.tickers();
        for (String ticker : tickers) {
            FinnhubRequest request = new FinnhubRequest("subscribe", ticker);
            String jsonMessage = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(jsonMessage));
            System.out.println("Sent message: " + jsonMessage);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        FinnhubResponse response = objectMapper.readValue(payload, FinnhubResponse.class);

        if("trade".equals(response.type()) && response.type() != null) {
            //marketDataService.processTrades(response.data());
            System.out.println("Received message: " + payload);
        }
    }
}