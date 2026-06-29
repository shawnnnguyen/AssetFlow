package com.project3.AssetFlow.holdings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingPerformance(
        UUID holdingId,
        UUID assetId,
        BigDecimal currentMarketPrice,
        BigDecimal currentTotalValue,
        BigDecimal absoluteChange,
        BigDecimal percentageChange
) {
}
