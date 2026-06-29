package com.project3.AssetFlow.market.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TrackedStocksDTO(
        UUID assetId,
        String ticker,
        BigDecimal latestPrice
) {
}
