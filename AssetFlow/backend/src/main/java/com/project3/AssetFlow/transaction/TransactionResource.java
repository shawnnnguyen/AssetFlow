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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TransactionResource {

    private final TransactionService transactionService;

    @PostMapping(value = "/transactions")
    public ResponseEntity<TransactionResponse> recordTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.recordTransaction(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/users/{userId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getFullTradingsHistory(@PathVariable Long userId,
                                                                            @PageableDefault(size = 20,
                                                                                    sort = {"executedAt", "id"},
                                                                                    direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(transactionService.getFullTradingsHistory(userId, pageable));
    }

    @GetMapping(value = "/portfolios/{portfolioId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getPortfolioTradingHistory(@PathVariable Long portfolioId,
                                                                                @PageableDefault Pageable pageable) {

        return ResponseEntity.ok(transactionService.getPortfolioTradingHistory(portfolioId, pageable));
    }

    @GetMapping(value = "/portfolios/{portfolioId}/transactions/{ticker}")
    public ResponseEntity<Page<TransactionResponse>> getTradingHistoryForAsset(@PathVariable Long portfolioId,
                                                                               @PathVariable String ticker,
                                                                               @PageableDefault Pageable pageable) {

        return ResponseEntity.ok(transactionService.getTradingHistoryForAsset(portfolioId, ticker, pageable));
    }

    @GetMapping(value = "transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }
}
