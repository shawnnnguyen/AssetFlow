package com.project3.AssetFlow.holdings.dto;

import java.math.BigDecimal;

public record HoldingPerformance(
        Long holdingId,
        Long assetId,
        BigDecimal currentMarketPrice,
        BigDecimal currentTotalValue,
        BigDecimal absoluteChange,
        BigDecimal percentageChange
) {
}
