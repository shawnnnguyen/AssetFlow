package com.project3.AssetFlow.transaction;

import com.project3.AssetFlow.identity.securityConfig.UserPrincipal;
import com.project3.AssetFlow.transaction.dto.TransactionRequest;
import com.project3.AssetFlow.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionResource {

    private final TransactionService transactionService;

    @PostMapping("/portfolios/{portfolioId}/transactions")
    public ResponseEntity<TransactionResponse> recordTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody TransactionRequest request) {

        if (!portfolioId.equals(request.portfolioId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "portfolioId in path does not match portfolioId in request body");
        }

        TransactionResponse response = transactionService.recordTransaction(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/portfolios/{portfolioId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getPortfolioTradingHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @RequestParam(required = false) String ticker,
            @PageableDefault(size = 20, sort = {"executedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {

        if (StringUtils.hasText(ticker)) {
            return ResponseEntity.ok(transactionService.getTradingHistoryForAsset(
                    principal.getId(), portfolioId, ticker, pageable));
        }
        return ResponseEntity.ok(transactionService.getPortfolioTradingHistory(
                principal.getId(), portfolioId, pageable));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getGlobalTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String ticker,
            @PageableDefault(size = 20, sort = {"executedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(transactionService.getFullTradingsHistory(
                principal.getId(), ticker, pageable));
    }

    @GetMapping("/portfolios/{portfolioId}/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID portfolioId,
            @PathVariable UUID transactionId) {

        return ResponseEntity.ok(transactionService.getTransactionById(
                principal.getId(), portfolioId, transactionId));
    }
}
