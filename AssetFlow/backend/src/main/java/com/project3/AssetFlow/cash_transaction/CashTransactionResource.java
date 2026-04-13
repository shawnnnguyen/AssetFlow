package com.project3.AssetFlow.cash_transaction;

import com.project3.AssetFlow.cash_transaction.dto.CashTransactionRequest;
import com.project3.AssetFlow.cash_transaction.dto.CashTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CashTransactionResource {

    private final CashTransactionService cashTransactionService;

    @GetMapping(value="/cash-transactions/{userId}")
    public ResponseEntity<Page<CashTransactionResponse>> getAllTransactions(@PathVariable Long userId,
                                                                                    @PageableDefault(size = 20,
                                                                                    sort = {"executedAt", "id"},
                                                                                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cashTransactionService.getAllTransactions(userId, pageable));
    }

    @GetMapping(value="/cash-transactions/{transactionId}")
    public ResponseEntity<CashTransactionResponse> getTransactionById(@PathVariable Long transactionId) {

        return ResponseEntity.ok(cashTransactionService.getTransactionById(transactionId));
    }

    @GetMapping(value="/cash-transactions/portfolio/{portfolioId}")
    public ResponseEntity<Page<CashTransactionResponse>> getTransactionsByPortfolio(@PathVariable Long portfolioId,
                                                                                    @PageableDefault Pageable pageable) {

        return ResponseEntity.ok(cashTransactionService.getTransactionsByPortfolio(portfolioId, pageable));
    }

    @PostMapping(value="/cash-transactions")
    public ResponseEntity<CashTransactionResponse> createTransaction(@RequestBody CashTransactionRequest request) {

        CashTransactionResponse response = cashTransactionService.recordCashTransaction(request);

        return ResponseEntity.status(201).body(response);
    }
}
