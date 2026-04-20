package com.project3.AssetFlow.holdings.dto;

import java.math.BigDecimal;

public record HoldingResponse(
        Long holdingId,
        Long assetId,
        Long portfolioId,

        BigDecimal quantity,
        BigDecimal avgCost,
        BigDecimal totalCost
) {
}
