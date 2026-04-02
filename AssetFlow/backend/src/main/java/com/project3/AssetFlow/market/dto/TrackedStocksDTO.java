package com.project3.AssetFlow.market.dto;

import java.math.BigDecimal;

public record TrackedStocksDTO(
        Long assetId,
        String ticker,
        BigDecimal latestPrice
) {
}
