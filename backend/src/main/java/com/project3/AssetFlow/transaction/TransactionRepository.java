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
    WHERE user_id = :userId
    AND (:assetId IS NULL OR asset_id = :assetId)
""", countQuery = """
    SELECT COUNT(*) FROM transactions
    WHERE user_id = :userId
    AND (:assetId IS NULL OR asset_id = :assetId)
""", nativeQuery = true)
    Page<Transaction> searchAllTransactions(
            @Param("userId") Long userId,
            @Param("assetId") Long assetId,
            Pageable pageable);
}
