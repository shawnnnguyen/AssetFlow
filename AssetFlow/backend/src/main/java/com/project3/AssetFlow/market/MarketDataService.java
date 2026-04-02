package com.project3.AssetFlow.market;

import com.project3.AssetFlow.market.dto.AssetInfoDTO;
import com.project3.AssetFlow.market.dto.FinnhubClient;
import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import com.project3.AssetFlow.streaming.dto.FinnHubTrade;
import com.project3.AssetFlow.streaming.dto.MarketUpdateDTO;
import com.project3.AssetFlow.streaming.handler.FinnhubWebSocketHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {

    private final PriceRepository priceRepository;
    private final AssetRepository assetRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FinnhubClient finnhubClient;
    private final FinnhubWebSocketHandler handler;

    private final Map<String, TrackedStocksDTO> liveCache = new ConcurrentHashMap<>();

    public MarketDataService(PriceRepository priceRepository,
                             AssetRepository assetRepository,
                             SimpMessagingTemplate messagingTemplate,
                             FinnhubClient finnhubClient,
                             FinnhubWebSocketHandler handler) {
        this.priceRepository = priceRepository;
        this.assetRepository = assetRepository;
        this.messagingTemplate = messagingTemplate;
        this.finnhubClient = finnhubClient;
        this.handler = handler;
    }

    @PostConstruct
    public void initCache() {
        List<TrackedStocksDTO> stocksDTOS = assetRepository.findAllTrackedAssetsWithLatestPrice();
        stocksDTOS.forEach(stocksDTO -> {
            liveCache.put(stocksDTO.ticker(),  stocksDTO);
        });
    }

    @Transactional
    public void processTrades(FinnHubTrade[] trades) {
        List<Price> pricesToSave = new ArrayList<>();

        for(FinnHubTrade trade : trades) {
            String ticker = trade.ticker();
            TrackedStocksDTO cachedStock = liveCache.get(ticker);

            if(cachedStock != null) {
                liveCache.put(ticker, new TrackedStocksDTO(
                        cachedStock.assetId(),
                        ticker,
                        trade.price()
                ));

                Price newPrice = new Price();
                newPrice.setPrice(trade.price());

                Asset assetRef = new Asset();
                assetRef.setId(cachedStock.assetId());
                newPrice.setAsset(assetRef);

                pricesToSave.add(newPrice);
            }
            messagingTemplate.convertAndSend("/topic/market/" + ticker,
                    new MarketUpdateDTO(ticker, trade.price()));
        }
        if (!pricesToSave.isEmpty()) priceRepository.saveAll(pricesToSave);
    }

    public boolean getCompanyProfile (String ticker) {
       AssetInfoDTO companyProfile = finnhubClient.getCompanyProfile(ticker);

       return companyProfile != null;
    }

    @Transactional
    public boolean addStockToTracking(String ticker) {
        if(liveCache.containsKey(ticker)) return true;

        AssetInfoDTO profile = finnhubClient.getCompanyProfile(ticker);
        if(profile == null) return false;

        Asset asset = assetRepository.findByTicker(ticker)
                .orElseGet(() -> {
                    Asset newAsset = new Asset();
                    newAsset.setTicker(ticker);
                    newAsset.setAssetName(profile.name());
                    newAsset.setCountry(profile.country());
                    newAsset.setIndustry(profile.industry());
                    return assetRepository.save(newAsset);
                });

        liveCache.put(ticker, new TrackedStocksDTO(
                asset.getId(),
                ticker,
                BigDecimal.ZERO
        ));

        handler.subscribeToTicker(ticker);
        return true;
    }

    public boolean removeStockFromTracking(String ticker) {
        if (ticker == null || ticker.isBlank()) return false;

        if(liveCache.containsKey(ticker)) {
            liveCache.remove(ticker);
            handler.unsubscribeFromTicker(ticker);
            return true;
        }
        return false;
    }

    public List<TrackedStocksDTO> getAllTrackedStocks() {
        return new  ArrayList<>(liveCache.values());
    }
}
