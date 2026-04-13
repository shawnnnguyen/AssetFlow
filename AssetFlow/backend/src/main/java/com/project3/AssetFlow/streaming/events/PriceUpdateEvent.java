package com.project3.AssetFlow.streaming.events;

import java.math.BigDecimal;

public record PriceUpdateEvent(
        Long assetId,
        BigDecimal latestPrice
) {
}
