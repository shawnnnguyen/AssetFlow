package com.project3.AssetFlow.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    ExchangeRate findByFromCurrencyCodeAndToCurrencyCode(String fromCurrency, String toCurrency);

    @Query("SELECT r FROM ExchangeRate r JOIN FETCH r.fromCurrency JOIN FETCH r.toCurrency")
    List<ExchangeRate> findAllWithCurrencies();
}
