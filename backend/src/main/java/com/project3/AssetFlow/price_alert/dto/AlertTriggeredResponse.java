package com.project3.AssetFlow.price_alert.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertTriggeredResponse(
    UUID alertTriggeredId,
    UUID alertId,
    UUID userId,
    UUID assetId,
    BigDecimal triggeredPrice,
    Instant triggeredAt
) {
}
