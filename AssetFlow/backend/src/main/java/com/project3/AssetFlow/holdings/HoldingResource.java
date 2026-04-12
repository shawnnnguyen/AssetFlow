package com.project3.AssetFlow.holdings;

import com.project3.AssetFlow.holdings.dto.HoldingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HoldingResource {

    private final HoldingService holdingService;

    @GetMapping(value = "/portfolios/{portfolio_id}/holdings/{ticker}")
    public ResponseEntity<List<HoldingDTO>> getHoldingsByTicker(@PathVariable Long portfolioId,
                                                                @PathVariable String ticker) {
        return ResponseEntity.ok(holdingService.getHoldingsByTicker(ticker, portfolioId));
    }

    @GetMapping(value = "/{userId}/portfolios/{portfolio_id}/holdings")
    public ResponseEntity<List<HoldingDTO>> getHoldingsByPortfolioId(@PathVariable Long userId,
                                                                     @PathVariable Long portfolioId) {
        return ResponseEntity.ok(holdingService.getHoldingsByPortfolioId(userId, portfolioId));
    }
}
