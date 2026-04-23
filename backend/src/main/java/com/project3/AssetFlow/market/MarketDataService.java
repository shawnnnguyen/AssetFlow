package com.project3.AssetFlow.market;

import com.project3.AssetFlow.currency.CurrencyRepository;
import com.project3.AssetFlow.currency.Currency;
import com.project3.AssetFlow.market.dto.AssetInfoDTO;
import com.project3.AssetFlow.market.dto.FinnhubClient;
import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import com.project3.AssetFlow.streaming.dto.FinnHubTrade;
import com.project3.AssetFlow.streaming.dto.MarketUpdateDTO;
import com.project3.AssetFlow.streaming.events.PriceUpdateEvent;
import com.project3.AssetFlow.streaming.handler.FinnhubSubscriptionManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final PriceRepository priceRepository;
    private final AssetRepository assetRepository;
    private final CurrencyRepository currencyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FinnhubClient finnhubClient;
    private final FinnhubSubscriptionManager subscriptionManager;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, TrackedStocksDTO> liveCache = new ConcurrentHashMap<>();

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
        List<MarketUpdateDTO> updates = new ArrayList<>();

        for(FinnHubTrade trade : trades) {
            String ticker = trade.ticker();
            TrackedStocksDTO cachedStock = liveCache.get(ticker);

            if (cachedStock == null) continue;
            BigDecimal oldPrice = cachedStock.latestPrice();
            BigDecimal newPrice = trade.price();

            if(oldPrice == null || oldPrice.compareTo(newPrice) != 0) {
                liveCache.put(ticker, new TrackedStocksDTO(
                        cachedStock.assetId(),
                        ticker,
                        newPrice
                ));


                Price priceEntity = new Price();
                priceEntity.setPrice(trade.price());
                priceEntity.setRecordedAt(Instant.ofEpochMilli(trade.timestamp()));

                Asset assetRef = assetRepository.getReferenceById(cachedStock.assetId());
                priceEntity.setAsset(assetRef);

                pricesToSave.add(priceEntity);
                updates.add(new MarketUpdateDTO(ticker, trade.price()));
            }

            if (oldPrice != null && oldPrice.compareTo(trade.price()) != 0) {
                eventPublisher.publishEvent(new PriceUpdateEvent(cachedStock.assetId(), oldPrice, trade.price()));
            }
        }

        if (!pricesToSave.isEmpty()) {
            priceRepository.saveAll(pricesToSave);

            for (MarketUpdateDTO update : updates) {
                messagingTemplate.convertAndSend("/topic/market/" + update.ticker(), update);
            }
        }
    }

    public EntityStatus getCompanyProfile (String ticker) {
        AssetInfoDTO companyProfile = finnhubClient.getCompanyProfile(ticker);

        if(companyProfile == null) return EntityStatus.NOT_FOUND;
        if(liveCache.containsKey(ticker)) return EntityStatus.ALREADY_EXISTS;

        return EntityStatus.FOUND;
    }

    @Transactional
    public EntityStatus addStockToTracking(String ticker) {
        if(liveCache.containsKey(ticker)) return EntityStatus.ALREADY_EXISTS;

        AssetInfoDTO profile = finnhubClient.getCompanyProfile(ticker);
        if(profile == null) return EntityStatus.NOT_FOUND;

        Currency stockNativeCurrency = currencyRepository.findByCode(profile.currencyCode());

        Asset asset = assetRepository.findByTicker(ticker)
                .orElseGet(() -> {
                    Asset newAsset = new Asset();
                    newAsset.setTicker(ticker);
                    newAsset.setAssetName(profile.name());
                    newAsset.setCurrency(stockNativeCurrency);
                    newAsset.setCountry(profile.country());
                    newAsset.setIndustry(profile.industry());
                    return assetRepository.save(newAsset);
                });

        liveCache.put(ticker, new TrackedStocksDTO(
                asset.getId(),
                ticker,
                null
        ));

        subscriptionManager.subscribeToTicker(ticker);
        return EntityStatus.ADDED;
    }

    public EntityStatus removeStockFromTracking(String ticker) {
        if (ticker == null || ticker.isBlank()) return EntityStatus.INVALID;

        if (!liveCache.containsKey(ticker)) {
            return EntityStatus.NOT_FOUND;
        }

        liveCache.remove(ticker);
        subscriptionManager.unsubscribeFromTicker(ticker);

        return EntityStatus.REMOVED;
    }

    public List<TrackedStocksDTO> getAllTrackedStocks() {
        return new ArrayList<>(liveCache.values());
    }
}
