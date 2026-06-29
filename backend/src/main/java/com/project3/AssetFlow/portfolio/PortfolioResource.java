package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.identity.securityConfig.UserPrincipal;
import com.project3.AssetFlow.portfolio.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios")
@RequiredArgsConstructor
public class PortfolioResource {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(portfolioService.getAllPortfoliosByUserId(principal.getId()));
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(portfolioService.getPortfolioById(principal.getId(), portfolioId));
    }

    @PostMapping
    public ResponseEntity<NewPortfolioResponse> addNewPortfolio(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NewPortfolioRequest portfolioRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.addNewPortfolio(portfolioRequest, principal.getId()));
    }

    @PatchMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest portfolioRequest) {
        return ResponseEntity.ok(
                portfolioService.updateVerifiedPortfolio(portfolioRequest, principal.getId(), portfolioId));
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> closePortfolio(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(portfolioService.closePortfolio(principal.getId(), portfolioId));
    }

    @GetMapping("/{portfolioId}/performance")
    public ResponseEntity<PortfolioPerformanceResponse> getPortfolioPerformance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(portfolioService.getPortfolioPerformance(principal.getId(), portfolioId));
    }
}
