package com.project3.AssetFlow.cash_transaction;

import com.project3.AssetFlow.cash_transaction.dto.CashTransactionRequest;
import com.project3.AssetFlow.cash_transaction.dto.CashTransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("portfolios/{portfolioId}/cash-transactions")
@RequiredArgsConstructor
public class CashTransactionResource {

    private final CashTransactionService cashTransactionService;

    @GetMapping
    public ResponseEntity<Page<CashTransactionResponse>> getTransactionsByPortfolio(
            @PathVariable Long portfolioId,
            @PageableDefault(size = 20, sort = "executedAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashTransactionService.getTransactionsByPortfolio(portfolioId, pageable));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<CashTransactionResponse> getTransactionById(@PathVariable Long transactionId) {

        return ResponseEntity.ok(cashTransactionService.getTransactionById(transactionId));
    }

    @PostMapping
    public ResponseEntity<CashTransactionResponse> recordTransaction(
            @Valid @RequestBody CashTransactionRequest request) {

        CashTransactionResponse response = cashTransactionService.recordCashTransaction(request);

        return ResponseEntity.status(201).body(response);
    }
}
