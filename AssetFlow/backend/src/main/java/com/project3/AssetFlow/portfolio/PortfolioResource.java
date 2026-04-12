package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.portfolio.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PortfolioResource {

    private final PortfolioService portfolioService;

    @GetMapping(value = "/{userId}/portfolios")
    public ResponseEntity<List<PortfolioDTO>> getAllPortfoliosByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.getAllPortfoliosByUserId(userId));
    }

    @GetMapping(value = "/{userId}/portfolios/{portfolioId}")
    public ResponseEntity<PortfolioDTO> getPortfolioById(@PathVariable Long userId,
                                                         @PathVariable Long portfolioId) {
        return ResponseEntity.ok(portfolioService.getPortfolioById(userId, portfolioId));
    }

    @PostMapping(value = "/{userId}/portfolios")
    public ResponseEntity<PortfolioResponse> addNewPortfolio(@PathVariable Long userId,
                                                             @RequestBody NewPortfolioRequest portfolioRequest) {
        PortfolioResponse response = portfolioService.addNewPortfolio(portfolioRequest, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping(value = "/{userId}/portfolios/{portfolioId}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(@RequestBody UpdatePortfolioRequest portfolioRequest,
                                                             @PathVariable Long userId,
                                                             @PathVariable Long portfolioId) {
        return portfolioService.updateVerifiedPortfolio(portfolioRequest, userId, portfolioId)
                .map(response -> ResponseEntity.ok(response))
                .orElse( ResponseEntity.noContent().build());
    }

    @PatchMapping(value = "/{userId}/portfolios/{portfolioId}", params = "close")
    public ResponseEntity<PortfolioDTO> closePortfolio(@PathVariable Long userId,
                                                        @PathVariable Long portfolioId) {
        return ResponseEntity.ok(portfolioService.closePortfolio(userId, portfolioId));
    }

    @GetMapping(value = "/portfolios/{portfolioId}/performance")
    public ResponseEntity<PortfolioPerformanceResponse> getPortfolioPerformance(@PathVariable Long portfolioId) {
        return ResponseEntity.ok(portfolioService.getPortfolioPerformance(portfolioId));
    }
}
