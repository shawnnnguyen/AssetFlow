package com.project3.AssetFlow.cash_transaction;

import com.project3.AssetFlow.cash_transaction.dto.CashTransactionRequest;
import com.project3.AssetFlow.cash_transaction.dto.CashTransactionResponse;
import com.project3.AssetFlow.portfolio.Portfolio;
import com.project3.AssetFlow.portfolio.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CashTransactionService {

    private final CashTransactionRepository cashTransactionRepository;
    private final PortfolioRepository portfolioRepository;

    @Transactional
    public CashTransactionResponse recordCashTransaction(CashTransactionRequest request) {

        Portfolio portfolio = portfolioRepository.findByIdForUpdate(request.portfolioId())
                .orElseThrow();

        BigDecimal cashBalance = portfolio.getCashBalance();

        switch (request.type()) {
            case DEPOSIT -> portfolio.setCashBalance(cashBalance.add(request.amount()));

            case WITHDRAWAL -> {
                if (cashBalance.compareTo(request.amount()) < 0) {
                    throw new IllegalStateException("Insufficient cash balance for withdrawal");
                }
                portfolio.setCashBalance(cashBalance.subtract(request.amount()));
            }

            default -> throw new IllegalArgumentException("Unsupported transaction type: " + request.type());
        }

        CashTransaction newTransaction = new CashTransaction();
        newTransaction.setUser(portfolio.getUser());
        newTransaction.setPortfolio(portfolio);
        newTransaction.setType(request.type());
        newTransaction.setAmount(request.amount());
        newTransaction.setExecutedAt(Instant.now());

        cashTransactionRepository.save(newTransaction);

        return mapToTransactionResponse(newTransaction);
    }

    public Page<CashTransactionResponse> getTransactionsByPortfolio(Long portfolioId, Pageable pageable) {
        Page<CashTransaction> transactions = cashTransactionRepository.findByPortfolioId(portfolioId, pageable);

        return transactions.map(this::mapToTransactionResponse);
    }

    public CashTransactionResponse getTransactionById(Long transactionId) {
        CashTransaction transaction = cashTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found"));

        return mapToTransactionResponse(transaction);
    }

    private CashTransactionResponse mapToTransactionResponse(CashTransaction transaction) {
        return new CashTransactionResponse(
                transaction.getUser().getId(),
                transaction.getPortfolio().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getExecutedAt()
        );
    }
}
