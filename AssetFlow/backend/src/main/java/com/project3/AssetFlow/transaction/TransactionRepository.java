package com.project3.AssetFlow.transaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByPortfolioId(Long portfolioId, Pageable pageable);

    Page<Transaction> findByAssetIdAndPortfolioId(Long assetId,
                                                  Long portfolioId,
                                                  Pageable pageable);

    @Query(value = """
    SELECT * FROM transactions 
    WHERE portfolio_id IS NULL OR portfolio_id = :portfolioId
    AND asset_id IS NULL OR asset_id= :assetId
""", nativeQuery = true)
    Page<Transaction> searchAllTransactions(
            @Param("portfolioId") Long portfolioId,
            @Param("assetId") Long assetId,
            Pageable pageable);
}
