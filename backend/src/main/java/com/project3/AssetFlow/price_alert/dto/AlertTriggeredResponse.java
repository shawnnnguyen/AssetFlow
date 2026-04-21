package com.project3.AssetFlow.price_alert.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertTriggeredResponse(
    Long alertTriggeredId,
    Long alertId,
    Long userId,
    Long assetId,
    BigDecimal triggeredPrice,
    Instant triggeredAt
) {
}
