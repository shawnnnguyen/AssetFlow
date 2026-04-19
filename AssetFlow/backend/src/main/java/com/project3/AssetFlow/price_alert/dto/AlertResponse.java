package com.project3.AssetFlow.price_alert.dto;

import java.math.BigDecimal;

public record AlertResponse(
        Long priceAlertId,
        Long userId,
        Long assetId,
        BigDecimal targetPrice
) {
}
