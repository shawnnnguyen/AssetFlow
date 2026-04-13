package com.project3.AssetFlow.currency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal convertCurrency(String fromCurrency, String toCurrency, BigDecimal amount) {

        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        ExchangeRate exchangeRate = exchangeRateRepository
                .findByFromCurrencyAndToCurrency(fromCurrency.toUpperCase(), toCurrency.toUpperCase());

        return amount.multiply(exchangeRate.getRate());
    }
}
