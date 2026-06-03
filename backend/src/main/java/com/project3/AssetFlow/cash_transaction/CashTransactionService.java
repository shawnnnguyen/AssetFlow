package com.project3.AssetFlow.cash_transaction;

import com.project3.AssetFlow.cash_transaction.dto.CashTransactionRequest;
import com.project3.AssetFlow.cash_transaction.dto.CashTransactionResponse;
import com.project3.AssetFlow.portfolio.Portfolio;
import com.project3.AssetFlow.portfolio.PortfolioRepository;
import com.project3.AssetFlow.streaming.events.PortfolioCashChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CashTransactionService {

    private final CashTransactionRepository cashTransactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Caching(evict = {
            @CacheEvict(value = "portfolio",  key = "#userId + ':' + #request.portfolioId"),
            @CacheEvict(value = "portfolios", key = "#userId")
    })
    @Transactional
    public CashTransactionResponse recordCashTransaction(Long userId, CashTransactionRequest request) {
        // Pre-lock ownership check — no lock held yet
        Portfolio portfolioSnapshot = portfolioRepository.findByIdWithDetails(request.portfolioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));

        if (!portfolioSnapshot.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Portfolio does not belong to the current user");
        }

        // Locked section — only cash balance reads/writes happen here
        Portfolio portfolio = portfolioRepository.findByIdForUpdate(request.portfolioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));

        BigDecimal cashBalance = portfolio.getCashBalance();

        switch (request.type()) {
            case DEPOSIT -> portfolio.setCashBalance(cashBalance.add(request.amount()));

            case WITHDRAWAL -> {
                if (cashBalance.compareTo(request.amount()) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient cash balance for withdrawal");
                }
                portfolio.setCashBalance(cashBalance.subtract(request.amount()));
            }

            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported transaction type: " + request.type());
        }

        CashTransaction newTransaction = new CashTransaction();
        newTransaction.setUser(portfolioSnapshot.getUser());
        newTransaction.setPortfolio(portfolio);
        newTransaction.setType(request.type());
        newTransaction.setAmount(request.amount());
        newTransaction.setExecutedAt(Instant.now());

        cashTransactionRepository.save(newTransaction);

        eventPublisher.publishEvent(new PortfolioCashChangedEvent(portfolio.getId()));

        return mapToTransactionResponse(newTransaction);
    }

    @Transactional(readOnly = true)
    public Page<CashTransactionResponse> getAllCashTransactions(Long userId, Pageable pageable) {
        return cashTransactionRepository.findByUserId(userId, pageable)
                .map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public Page<CashTransactionResponse> getTransactionsByPortfolio(Long userId, Long portfolioId, Pageable pageable) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Portfolio does not belong to the current user");
        }
        return cashTransactionRepository.findByPortfolioId(portfolioId, pageable)
                .map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public CashTransactionResponse getTransactionById(Long userId, Long portfolioId, Long transactionId) {
        CashTransaction transaction = cashTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transaction does not belong to the current user");
        }
        if (!transaction.getPortfolio().getId().equals(portfolioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found in this portfolio");
        }
        return mapToTransactionResponse(transaction);
    }

    private CashTransactionResponse mapToTransactionResponse(CashTransaction transaction) {
        return new CashTransactionResponse(
                transaction.getId(),
                transaction.getPortfolio().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getExecutedAt()
        );
    }
}
