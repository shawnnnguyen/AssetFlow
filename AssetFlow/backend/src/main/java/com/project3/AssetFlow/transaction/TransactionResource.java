package com.project3.AssetFlow.transaction;

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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TransactionResource {

    private final TransactionService transactionService;

    @PostMapping("portfolios/{portfolioId}/transactions")
    public ResponseEntity<TransactionResponse> recordTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.recordTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("portfolios/{portfolioId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getPortfolioTradingHistory(
            @RequestParam Long portfolioId,
            @RequestParam(required = false) String ticker,
            @PageableDefault(size = 20, sort = {"executedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {

        if (StringUtils.hasText(ticker)) {
            return ResponseEntity.ok(transactionService.getTradingHistoryForAsset(portfolioId, ticker, pageable));
        }
        return ResponseEntity.ok(transactionService.getPortfolioTradingHistory(portfolioId, pageable));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getGlobalTransactions(
            @RequestParam(required = false) Long portfolioId,
            @RequestParam(required = false) String ticker,
            @PageableDefault(size = 20, sort = {"executedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(transactionService.getFullTradingsHistory(portfolioId, ticker, pageable));
    }

    @GetMapping("portfolios/{portfolioId}/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }
}
