package com.project3.AssetFlow.market;

import com.project3.AssetFlow.streaming.dto.FinnHubTrade;
import com.project3.AssetFlow.streaming.dto.MarketUpdateDTO;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;

@Service
public class MarketDataService {

    private PriceRepository priceRepository;
    private AssetRepository assetRepository;
    private SimpMessagingTemplate messagingTemplate;

    public MarketDataService(PriceRepository priceRepository,
                             AssetRepository assetRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.priceRepository = priceRepository;
        this.assetRepository = assetRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void processTrades(FinnHubTrade[] trades)
    {
        Arrays.stream(trades).forEach(trade -> {
            System.out.print("Processing trade: " + trade);

            Asset asset = assetRepository.findByTicker(trade.ticker());

            if (asset == null) {
                asset = new Asset();
                asset.setTicker(trade.ticker());
                asset = assetRepository.save(asset); // Reassign to get the auto-generated ID
                System.out.println("Created new asset in database: " + trade.ticker());
            }

            Instant timestamp = Instant.ofEpochMilli(trade.timestamp());

            Price price = new Price();
            price.setAsset(asset);
            price.setPrice(trade.price());
            price.setRecordedAt(timestamp);
            priceRepository.save(price);

            messagingTemplate.convertAndSend("/topic/market" + trade.ticker(),
                                                new MarketUpdateDTO(trade.ticker(), price.getPrice()));
        });
    }
}
