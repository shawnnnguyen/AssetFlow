package com.project3.AssetFlow.price_alert.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AlertResponse(
        UUID priceAlertId,
        UUID userId,
        UUID assetId,
        String ticker,
        BigDecimal targetPrice
) {
}
