package com.project3.AssetFlow.transaction;

import com.project3.AssetFlow.holdings.Holding;
import com.project3.AssetFlow.holdings.HoldingRepository;
import com.project3.AssetFlow.market.*;
import com.project3.AssetFlow.portfolio.Portfolio;
import com.project3.AssetFlow.portfolio.PortfolioRepository;
import com.project3.AssetFlow.transaction.dto.TransactionRequest;
import com.project3.AssetFlow.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PriceRepository priceRepository;
    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;

    @Transactional
    public TransactionResponse recordTransaction(TransactionRequest request) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow();
        Portfolio portfolio = portfolioRepository.findByIdForUpdate(request.portfolioId())
                .orElseThrow();

        Price executedPrice = priceRepository.findPriceAsOf(request.assetId(), request.executedAt())
                .orElseThrow(() -> new IllegalStateException("No price found for asset " + request.assetId()));

        BigDecimal cashBalance = portfolio.getCashBalance();
        Holding updatedHolding = holdingRepository.findByPortfolioIdAndAssetIdForUpdate(request.portfolioId(), request.assetId());

        BigDecimal transactionValue = request.quantity().multiply(executedPrice.getPrice());

        if (request.type() == TransactionType.BUY) {
            if(cashBalance.compareTo(transactionValue) < 0) {
                throw new IllegalStateException("Insufficient cash balance");
            }

            if (updatedHolding == null || updatedHolding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {

                if (updatedHolding == null) updatedHolding = new Holding();
                updatedHolding.setPortfolio(portfolio);
                updatedHolding.setAsset(asset);
                updatedHolding.setQuantity(request.quantity());
                updatedHolding.setAvgCost(executedPrice.getPrice());
            } else {
                BigDecimal oldCost = updatedHolding.getQuantity().multiply(updatedHolding.getAvgCost());
                BigDecimal newQuantity = updatedHolding.getQuantity().add(request.quantity());
                BigDecimal newAvgCost = oldCost.add(transactionValue).divide(newQuantity, 4, RoundingMode.HALF_UP);

                updatedHolding.setQuantity(newQuantity);
                updatedHolding.setAvgCost(newAvgCost);
            }

            portfolio.setCashBalance(cashBalance.subtract(transactionValue));
            holdingRepository.save(updatedHolding);
        }
        else if (request.type() == TransactionType.SELL) {
            if(updatedHolding == null || updatedHolding.getQuantity().compareTo(request.quantity()) < 0) {
                throw new IllegalStateException("Insufficient holding quantity");
            }

            updatedHolding.setQuantity(updatedHolding.getQuantity().subtract(request.quantity()));

            portfolio.setCashBalance(cashBalance.add(transactionValue));
            holdingRepository.save(updatedHolding);
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setPortfolio(portfolio);
        newTransaction.setAsset(asset);
        newTransaction.setQuantity(request.quantity());
        newTransaction.setPricePerUnit(executedPrice.getPrice());
        newTransaction.setExecutedAt(request.executedAt());
        newTransaction.setType(request.type());
        transactionRepository.save(newTransaction);

        return mapToTransactionResponse(newTransaction);
    }

    public Page<TransactionResponse> getFullTradingsHistory(Long userId, Pageable pageable) {
        Page<Transaction> history = transactionRepository.findByUserId(userId, pageable);

        return history.map(this::mapToTransactionResponse);
    }

    public Page<TransactionResponse> getPortfolioTradingHistory(Long portfolioId, Pageable pageable) {
        Page<Transaction> history = transactionRepository.findByPortfolioId(portfolioId, pageable);

        return history.map(this::mapToTransactionResponse);
    }

    public Page<TransactionResponse> getTradingHistoryForAsset(Long portfolioId,
                                                               String ticker,
                                                               Pageable pageable) {
        Asset asset = assetRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalStateException("Asset not found"));

        Page<Transaction> history = transactionRepository.findByAssetIdAndPortfolioId(asset.getId(), portfolioId, pageable);

        return history.map(this::mapToTransactionResponse);
    }

    public TransactionResponse getTransactionById(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found"));

        return mapToTransactionResponse(transaction);
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getPortfolio().getId(),
                transaction.getAsset().getId(),
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getExecutedAt(),
                transaction.getType()
        );
    }
}
