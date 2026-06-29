package com.project3.AssetFlow.streaming.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceUpdateEvent(
        UUID assetId,
        BigDecimal oldPrice,
        BigDecimal latestPrice
) {
}
