package com.project3.AssetFlow.holdings.dto;

import java.math.BigDecimal;

public record HoldingDTO(
        Long assetId,
        Long portfolioId,

        BigDecimal quantity,
        BigDecimal avgCost,
        BigDecimal totalCost
) {
}
