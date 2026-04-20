package com.project3.AssetFlow.currency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

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
            BigDecimal calculatedRate = BigDecimal.ONE.divide(inverseRate.getRate(),4, RoundingMode.HALF_UP);
            return amount.multiply(calculatedRate);
        }

        BigDecimal toBaseCurrency = exchangeRateRepository.findByFromCurrencyCodeAndToCurrencyCode(fromCurrency.toUpperCase(), "USD").getRate();
        BigDecimal fromBaseCurrency = exchangeRateRepository.findByFromCurrencyCodeAndToCurrencyCode("USD", toCurrency.toUpperCase()).getRate();
        BigDecimal calculatedRate = toBaseCurrency.multiply(fromBaseCurrency);
        return amount.multiply(calculatedRate);
    }
}
