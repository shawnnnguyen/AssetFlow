package com.project3.AssetFlow.currency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
    Currency findByCode(String code);

    Currency findBySymbol(String symbol);
}
