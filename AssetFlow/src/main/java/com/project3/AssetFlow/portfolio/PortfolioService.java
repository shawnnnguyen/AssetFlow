package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.market.Asset;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PortfolioService {
    private static List<Asset> portfolio = new ArrayList<>();

    static {
        Asset asset1 = new Asset("AAPL", "Apple Inc.","USA", AssetType.STOCK);
        Asset asset2 = new Asset("MSFT", "Microsoft Corporation", "USA", AssetType.STOCK);
        Asset asset3 = new Asset("TSLA","Tesla Inc.", "USA", AssetType.STOCK);
        Asset asset4 = new Asset("VOO", "Vanguard S&P 500 ETF", "USA", AssetType.ETF);
        Asset asset5 = new Asset("NVDA","NVIDIA Corporation", "USA", AssetType.STOCK);

        portfolio.addAll(List.of(asset1, asset2, asset3, asset4, asset5));
    }

    private PortfolioRepository portfolioRepository;
    private HoldingRepository holdingRepository;
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
    public void executeTrade(Long portfolioId, Long assetId, BigDecimal quantity, TradeType type) {
        // 1. Fetch Portfolio and Asset Price
        // 2. If BUY: Check if (price * quantity) <= cashBalance
        // 3. If SELL: Check if holdings contains enough quantity
        // 4. Update Portfolio cash_balance
        // 5. Update/Create Holding record
        // 6. Log entry in Transactions table
        Portfolio pf = getPortfolioById(portfolioId);


    }

    public List<Asset> getAllAssets() {
        return portfolio;
    }

    public Asset getAssetById(Long id) {
        if(id == null) return null;

        return portfolio.stream()
                        .filter(transaction -> transaction.getId() == id)
                        .findFirst()
                        .orElse(null);
    }

    public void addNewAsset(Asset asset) {

    }
}
