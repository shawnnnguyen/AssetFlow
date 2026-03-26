package com.project3.AssetFlow.market;

import com.project3.AssetFlow.streaming.dto.FinnHubTrade;
import com.project3.AssetFlow.streaming.dto.MarketUpdateDTO;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

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

    public void processTrades(FinnHubTrade[] trades)
    {
        Arrays.stream(trades).forEach(trade -> {
            System.out.print("Processing trade: " + trade);

            Asset asset = assetRepository.findByTicker(trade.ticker());

            if (asset != null) {
                Price price = new Price();
                price.setAsset(asset);
                price.setPrice(trade.price());
                price.setRecordedAt(trade.timestamp());
                priceRepository.save(price);

                messagingTemplate.convertAndSend("/topic/market" + trade.ticker(),
                                                    new MarketUpdateDTO(trade.ticker(), price.getPrice()));
            }
        });
    }
}
