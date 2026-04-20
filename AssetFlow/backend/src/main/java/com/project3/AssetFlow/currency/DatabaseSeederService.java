package com.project3.AssetFlow.currency;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseSeederService {

    private final CurrencyRepository currencyRepo;
    private final ExchangeRateRepository rateRepo;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeederService.class);

    @Transactional
    public void seedData() {
        seedCurrencies();
        seedExchangeRates();
    }

    private void seedCurrencies() {
        if (currencyRepo.count() > 0) return;

        logger.info("Reading currencies from CSV...");
        List<Currency> currencies = new ArrayList<>();

        try (InputStreamReader isr = new InputStreamReader(new ClassPathResource("currencies.csv").getInputStream());
             CSVReader reader = new CSVReader(isr)) {

            String[] data;
            while ((data = reader.readNext()) != null) {
                if (data.length < 2) continue;

                try {
                    Currency currency = new Currency();
                    currency.setCode(data[0].trim());
                    currency.setSymbol(data[1].trim());

                    currencies.add(currency);
                } catch (Exception e) {
                    logger.warn("Skipping malformed currency row due to parsing error. Data: [{}]. Error: {}",
                            String.join(",", data), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error reading currencies from CSV: {}", e.getMessage(), e);
        }

        if (!currencies.isEmpty()) {
            currencyRepo.saveAll(currencies);
            logger.info("Currencies ingested successfully.");
        }
    }

    private void seedExchangeRates() {
        if (rateRepo.count() > 0) return;

        logger.info("Reading exchange rates from CSV...");
        List<ExchangeRate> rates = new ArrayList<>();

        try (InputStreamReader isr = new InputStreamReader(new ClassPathResource("exchange_rates.csv").getInputStream());
             CSVReader reader = new CSVReader(isr)) {

            String[] data;
            while ((data = reader.readNext()) != null) {
                if (data.length < 5) continue;

                try {
                    ExchangeRate rate = new ExchangeRate();

                    Currency fromCurrency = currencyRepo.findByCode(data[2].trim());
                    Currency toCurrency = currencyRepo.findByCode(data[3].trim());

                    rate.setRate(new BigDecimal(data[1].trim()));
                    rate.setFromCurrency(fromCurrency);
                    rate.setToCurrency(toCurrency);
                    rate.setUpdatedAt(Instant.parse(data[4].trim()));

                    rates.add(rate);
                } catch (Exception e) {
                    logger.warn("Skipping malformed row due to parsing error. Data: [{}]. Error: {}",
                            String.join(",", data), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error reading exchange rates from CSV: {}", e.getMessage(), e);
        }

        if (!rates.isEmpty()) {
            rateRepo.saveAll(rates);
            logger.info("Exchange rates ingested successfully.");
        }
    }
}