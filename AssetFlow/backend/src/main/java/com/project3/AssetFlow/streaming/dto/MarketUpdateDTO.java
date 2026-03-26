package com.project3.AssetFlow.streaming.dto;

import java.math.BigDecimal;

public record MarketUpdateDTO(
        String ticker,
        BigDecimal price
) {
}
