package com.project3.AssetFlow.currency;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private static final int EXCHANGE_RATE_SCALE = 4;

    private final ExchangeRateRepository exchangeRateRepository;

    private final ConcurrentHashMap<String, BigDecimal> rateCache = new ConcurrentHashMap<>();

    @PostConstruct
    @Transactional(readOnly = true)
    @Scheduled(fixedRate = 300_000)
    public void refreshCache() {
        ConcurrentHashMap<String, BigDecimal> fresh = new ConcurrentHashMap<>();
        exchangeRateRepository.findAllWithCurrencies().forEach(r ->
                fresh.put(r.getFromCurrency().getCode() + r.getToCurrency().getCode(), r.getRate()));
        rateCache.clear();
        rateCache.putAll(fresh);
    }

    public BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount) {
        String from = fromCurrency.toUpperCase();
        String to   = toCurrency.toUpperCase();

        if (from.equals(to)) {
            return amount;
        }

        BigDecimal direct = rateCache.get(from + to);
        if (direct != null) {
            return amount.multiply(direct);
        }

        BigDecimal inverse = rateCache.get(to + from);
        if (inverse != null) {
            BigDecimal rate = BigDecimal.ONE.divide(inverse, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
            return amount.multiply(rate);
        }

        BigDecimal toUsd   = rateCache.get(from + "USD");
        BigDecimal fromUsd = rateCache.get("USD" + to);
        if (toUsd == null || fromUsd == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "No exchange rate path found for " + fromCurrency + " to " + toCurrency);
        }
        return amount.multiply(toUsd.multiply(fromUsd));
    }
}
