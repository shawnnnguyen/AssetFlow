package com.project3.AssetFlow.market;

import com.project3.AssetFlow.streaming.dto.FinnHubTrade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

@Service
public class MarketDataService {

//    private AssetRepository assetRepository;
//    private PriceRepository priceRepository;
//
//    @Transactional
//    public void processTrades(FinnHubTrade[] trades) {
//        for (FinnHubTrade trade : trades) {
//            Optional<Asset> assetOpt = assetRepository.findByTicker(trade.ticker());
//
//            if (Arrays.stream(trades).noneMatch(t -> t.ticker().equals(trade.ticker()))) {
//                Asset newAsset = new Asset();
//                newAsset.setTicker(trade.ticker());
//                assetRepository.save(newAsset);
//            }
//
//            assetOpt.ifPresent(asset -> {
//                Price newPrice = new Price();
//                newPrice.setAsset(asset);
//                newPrice.setPrice(trade.price());
//                newPrice.setRecordedAt(trade.timestamp());
//
//                priceRepository.save(newPrice);
//            });
//        }
//    }
}
