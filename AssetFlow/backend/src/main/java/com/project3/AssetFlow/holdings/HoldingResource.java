package com.project3.AssetFlow.holdings;

import com.project3.AssetFlow.holdings.dto.HoldingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolios/{portfolioId}/holdings")
@RequiredArgsConstructor
public class HoldingResource {

    private final HoldingService holdingService;

    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getHoldingsByPortfolioId(
            @PathVariable Long portfolioId,
            @RequestParam(required = false) String ticker) {

        if(StringUtils.hasText(ticker)) {
            return ResponseEntity.ok(holdingService.getHoldingsByTicker(ticker, portfolioId));
        }

        return ResponseEntity.ok(holdingService.getHoldingsByPortfolioId(portfolioId));
    }
}
