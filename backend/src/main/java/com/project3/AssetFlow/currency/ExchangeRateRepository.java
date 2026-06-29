package com.project3.AssetFlow.currency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    ExchangeRate findByFromCurrencyCodeAndToCurrencyCode(String fromCurrency, String toCurrency);

    @Query("SELECT r FROM ExchangeRate r JOIN FETCH r.fromCurrency JOIN FETCH r.toCurrency")
    List<ExchangeRate> findAllWithCurrencies();
}
