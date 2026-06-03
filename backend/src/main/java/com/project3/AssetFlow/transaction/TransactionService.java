package com.project3.AssetFlow.transaction;

import com.project3.AssetFlow.currency.CurrencyConversionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final CurrencyConversionService currencyConversionService;

    @Transactional
    public TransactionResponse recordTransaction(Long userId, TransactionRequest request) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

        Portfolio portfolio = portfolioRepository.findByIdForUpdate(request.portfolioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
        verifyOwnership(portfolio, userId);

        Price executedPrice = priceRepository.findPriceAsOf(request.assetId(), request.executedAt())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "No price data found for asset " + request.assetId() + " at the requested time"));

        BigDecimal cashBalance = portfolio.getCashBalance();
        Holding updatedHolding = holdingRepository.findByPortfolioIdAndAssetIdForUpdate(
                request.portfolioId(), request.assetId());

        BigDecimal transactionValue = request.quantity().multiply(executedPrice.getPrice());
        BigDecimal transactionValueInPortfolioCurrency = currencyConversionService.convertCurrency(
                asset.getCurrency().getCode(), portfolio.getCurrency().getCode(), transactionValue);

        if (request.type() == TransactionType.BUY) {
            if (cashBalance.compareTo(transactionValueInPortfolioCurrency) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient cash balance");
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

            portfolio.setCashBalance(cashBalance.subtract(transactionValueInPortfolioCurrency));
            holdingRepository.save(updatedHolding);
        } else if (request.type() == TransactionType.SELL) {
            if (updatedHolding == null || updatedHolding.getQuantity().compareTo(request.quantity()) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient holding quantity");
            }

            updatedHolding.setQuantity(updatedHolding.getQuantity().subtract(request.quantity()));
            portfolio.setCashBalance(cashBalance.add(transactionValueInPortfolioCurrency));
            holdingRepository.save(updatedHolding);
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setPortfolio(portfolio);
        newTransaction.setUser(portfolio.getUser());
        newTransaction.setAsset(asset);
        newTransaction.setQuantity(request.quantity());
        newTransaction.setPricePerUnit(executedPrice.getPrice());
        newTransaction.setCurrency(asset.getCurrency());
        newTransaction.setExecutedAt(request.executedAt());
        newTransaction.setType(request.type());
        transactionRepository.save(newTransaction);

        return mapToTransactionResponse(newTransaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getPortfolioTradingHistory(Long userId, Long portfolioId, Pageable pageable) {
        verifyPortfolioOwnership(userId, portfolioId);
        return transactionRepository.findByPortfolioId(portfolioId, pageable)
                .map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTradingHistoryForAsset(Long userId, Long portfolioId,
                                                               String ticker, Pageable pageable) {
        verifyPortfolioOwnership(userId, portfolioId);
        Asset asset = assetRepository.findByTicker(ticker)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        return transactionRepository.findByAssetIdAndPortfolioId(asset.getId(), portfolioId, pageable)
                .map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getFullTradingsHistory(Long userId, String ticker, Pageable pageable) {
        Long assetId = null;
        if (StringUtils.hasText(ticker)) {
            assetId = assetRepository.findByTicker(ticker)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"))
                    .getId();
        }
        return transactionRepository.searchAllTransactions(userId, assetId, pageable)
                .map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long userId, Long portfolioId, Long transactionId) {
        verifyPortfolioOwnership(userId, portfolioId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!transaction.getPortfolio().getId().equals(portfolioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Transaction does not belong to this portfolio");
        }
        return mapToTransactionResponse(transaction);
    }

    private void verifyPortfolioOwnership(Long userId, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
        verifyOwnership(portfolio, userId);
    }

    private void verifyOwnership(Portfolio portfolio, Long userId) {
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Portfolio does not belong to the user");
        }
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getPortfolio().getId(),
                transaction.getAsset().getId(),
                transaction.getQuantity(),
                transaction.getPricePerUnit(),
                transaction.getCurrency().getCode(),
                transaction.getExecutedAt(),
                transaction.getType()
        );
    }
}
