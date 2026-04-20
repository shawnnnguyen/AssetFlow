package com.project3.AssetFlow.currency;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CsvDataSeeder {

    @Bean
    public CommandLineRunner loadCsvData(DatabaseSeederService seederService) {
        return args -> seederService.seedData();
    }
}
