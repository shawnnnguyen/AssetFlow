package com.project3.AssetFlow.currency;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CsvDataSeeder {

    private final CurrencyRepository currencyRepo;
    private final ExchangeRateRepository rateRepo;

    private static final Logger logger = LoggerFactory.getLogger(CsvDataSeeder.class);

    @Bean
    @Transactional
    public CommandLineRunner loadCsvData() {
        return args -> {

            if (currencyRepo.count() == 0) {
                logger.info("Reading currencies from CSV...");

                List<Currency> currencies = new ArrayList<>();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        new ClassPathResource("currencies.csv").getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] data = line.split(",");
                        if (data.length == 2) {
                            Currency currency = new Currency();
                            currency.setCode(data[0].trim());
                            currency.setSymbol(data[1].trim());
                            currencies.add(currency);
                        }
                    }
                }
                currencyRepo.saveAll(currencies);
                logger.info("Currencies ingested successfully.");
            }

            if (rateRepo.count() == 0) {
                logger.info("Reading exchange rates from CSV...");
                List<ExchangeRate> rates = new ArrayList<>();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        new ClassPathResource("exchange_rates.csv").getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] data = line.split(",");
                        if (data.length == 5) {
                            ExchangeRate rate = new ExchangeRate();

                            Currency fromCurrency = currencyRepo.findByCode(data[2].trim());
                            Currency toCurrency = currencyRepo.findByCode(data[3].trim());

                            rate.setRate(new BigDecimal(data[1].trim()));
                            rate.setFromCurrency(fromCurrency);
                            rate.setToCurrency(toCurrency);

                            rate.setUpdatedAt(Instant.parse(data[4].trim()));

                            rates.add(rate);
                        }
                    }
                }
                rateRepo.saveAll(rates);
                logger.info("Exchange rates ingested successfully.");
            }
        };
    }
}
