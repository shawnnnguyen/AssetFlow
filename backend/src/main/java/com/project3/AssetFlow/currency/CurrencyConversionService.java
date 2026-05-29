package com.project3.AssetFlow.currency;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private static final int EXCHANGE_RATE_SCALE = 4;

    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount) {

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        ExchangeRate exchangeRate = exchangeRateRepository
                .findByFromCurrencyCodeAndToCurrencyCode(fromCurrency.toUpperCase(), toCurrency.toUpperCase());

        if (exchangeRate != null) {
            return amount.multiply(exchangeRate.getRate());
        }

        ExchangeRate inverseRate = exchangeRateRepository
                .findByFromCurrencyCodeAndToCurrencyCode(toCurrency.toUpperCase(), fromCurrency.toUpperCase());

        if (inverseRate != null) {
            BigDecimal calculatedRate = BigDecimal.ONE.divide(inverseRate.getRate(), EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
            return amount.multiply(calculatedRate);
        }

        ExchangeRate toUsd = exchangeRateRepository.findByFromCurrencyCodeAndToCurrencyCode(fromCurrency.toUpperCase(), "USD");
        ExchangeRate fromUsd = exchangeRateRepository.findByFromCurrencyCodeAndToCurrencyCode("USD", toCurrency.toUpperCase());
        if (toUsd == null || fromUsd == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No exchange rate path found for " + fromCurrency + " to " + toCurrency);
        }
        BigDecimal calculatedRate = toUsd.getRate().multiply(fromUsd.getRate());
        return amount.multiply(calculatedRate);
    }
}
