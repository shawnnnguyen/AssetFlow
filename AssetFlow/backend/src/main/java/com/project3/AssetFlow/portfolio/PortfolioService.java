package com.project3.AssetFlow.portfolio;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    // private TransactionRepository transactionRepository;

    public Portfolio getPortfolioById(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));
    }

    public Holding getHoldingById(Long id) {
        return holdingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holding not found"));
    }

    @Transactional
    public Portfolio updateCashBalance(Long portfolioId, BigDecimal amount) {
        Portfolio pf = getPortfolioById(portfolioId);
        BigDecimal newBalance = pf.getCashBalance().add(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient funds for this operation");
        }

        pf.setCashBalance(newBalance);
        return portfolioRepository.save(pf);
    }

    @Transactional
    public void executeTrade(Long portfolioId, Long assetId, BigDecimal quantity, String type) {
        // 1. Fetch Portfolio and Asset Price
        // 2. If BUY: Check if (price * quantity) <= cashBalance
        // 3. If SELL: Check if holdings contains enough quantity
        // 4. Update Portfolio cash_balance
        // 5. Update/Create Holding record
        // 6. Log entry in Transactions table
        Portfolio pf = getPortfolioById(portfolioId);


    }

//    public List<Asset> getAllAssets() {
//
//    }
//
//    public Asset getAssetById(Long id) {
//
//    }
//
//    public void addNewAsset(Asset asset) {
//
//    }
}
