package com.project3.AssetFlow.cash_transaction;

import com.project3.AssetFlow.cash_transaction.dto.CashTransactionRequest;
import com.project3.AssetFlow.cash_transaction.dto.CashTransactionResponse;
import com.project3.AssetFlow.identity.securityConfig.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CashTransactionResource {

    private final CashTransactionService cashTransactionService;

    @GetMapping("/cash-transactions")
    public ResponseEntity<Page<CashTransactionResponse>> getGlobalCashTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 50, sort = "executedAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashTransactionService.getAllCashTransactions(principal.getId(), pageable));
    }

    @GetMapping("/portfolios/{portfolioId}/cash-transactions")
    public ResponseEntity<Page<CashTransactionResponse>> getTransactionsByPortfolio(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @PageableDefault(size = 20, sort = "executedAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashTransactionService.getTransactionsByPortfolio(
                principal.getId(), portfolioId, pageable));
    }

    @GetMapping("/portfolios/{portfolioId}/cash-transactions/{transactionId}")
    public ResponseEntity<CashTransactionResponse> getTransactionById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(cashTransactionService.getTransactionById(
                principal.getId(), portfolioId, transactionId));
    }

    @PostMapping("/portfolios/{portfolioId}/cash-transactions")
    public ResponseEntity<CashTransactionResponse> recordTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody CashTransactionRequest request) {

        if (!portfolioId.equals(request.portfolioId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "portfolioId in path does not match portfolioId in request body");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cashTransactionService.recordCashTransaction(principal.getId(), request));
    }
}
