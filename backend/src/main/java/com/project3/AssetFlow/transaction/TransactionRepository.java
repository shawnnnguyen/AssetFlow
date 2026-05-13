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

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND (:assetId IS NULL OR t.asset.id = :assetId)")
    Page<Transaction> searchAllTransactions(
            @Param("userId") Long userId,
            @Param("assetId") Long assetId,
            Pageable pageable);
}
