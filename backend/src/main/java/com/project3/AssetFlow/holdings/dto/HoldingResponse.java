package com.project3.AssetFlow.holdings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingResponse(
        UUID holdingId,
        UUID assetId,
        UUID portfolioId,
        BigDecimal quantity,
        BigDecimal avgCost,
        BigDecimal totalCost
) {
}
