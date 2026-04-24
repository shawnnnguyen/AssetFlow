package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.portfolio.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/portfolios")
@RequiredArgsConstructor
public class PortfolioResource {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfoliosByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.getAllPortfoliosByUserId(userId));
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(
            @PathVariable Long userId,
            @PathVariable Long portfolioId) {
        return ResponseEntity.ok(portfolioService.getPortfolioById(userId, portfolioId));
    }

    @PostMapping
    public ResponseEntity<NewPortfolioResponse> addNewPortfolio(
            @PathVariable Long userId,
            @RequestBody NewPortfolioRequest portfolioRequest) {
        NewPortfolioResponse response = portfolioService.addNewPortfolio(portfolioRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable Long userId,
            @PathVariable Long portfolioId,
            @RequestBody UpdatePortfolioRequest portfolioRequest) {
        return portfolioService.updateVerifiedPortfolio(portfolioRequest, userId, portfolioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> closePortfolio(
            @PathVariable Long userId,
            @PathVariable Long portfolioId) {
        return ResponseEntity.ok(portfolioService.closePortfolio(userId, portfolioId));
    }

    @GetMapping("/{portfolioId}/performance")
    public ResponseEntity<PortfolioPerformanceResponse> getPortfolioPerformance(
            @PathVariable Long portfolioId) {

        return ResponseEntity.ok(portfolioService.getPortfolioPerformance(portfolioId));
    }
}
