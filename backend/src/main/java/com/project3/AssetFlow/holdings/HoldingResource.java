package com.project3.AssetFlow.holdings;

import com.project3.AssetFlow.holdings.dto.HoldingResponse;
import com.project3.AssetFlow.identity.securityConfig.UserPrincipal;
import com.project3.AssetFlow.portfolio.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios/{portfolioId}/holdings")
@RequiredArgsConstructor
public class HoldingResource {

    private final HoldingService holdingService;
    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getHoldingsByPortfolioId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @RequestParam(required = false) String ticker) {

        portfolioService.getVerifiedPortfolio(principal.getId(), portfolioId);

        if(StringUtils.hasText(ticker)) {
            return ResponseEntity.ok(holdingService.getHoldingsByTicker(ticker, portfolioId));
        }

        return ResponseEntity.ok(holdingService.getHoldingsByPortfolioId(portfolioId));
    }
}
