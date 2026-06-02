package com.project3.AssetFlow.streaming.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean("priceWriteExecutor")
    public Executor priceWriteExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("PriceWrite-");
        ex.initialize();
        return ex;
    }

    @Bean("portfolioCalcExecutor")
    public Executor portfolioCalcExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(10);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("PortfolioCalc-");
        ex.initialize();
        return ex;
    }
}
