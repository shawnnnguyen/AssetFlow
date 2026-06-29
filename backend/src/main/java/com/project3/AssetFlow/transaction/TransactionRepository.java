package com.project3.AssetFlow.transaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByPortfolioId(UUID portfolioId, Pageable pageable);

    Page<Transaction> findByAssetIdAndPortfolioId(UUID assetId, UUID portfolioId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND (:assetId IS NULL OR t.asset.id = :assetId)")
    Page<Transaction> searchAllTransactions(
            @Param("userId") UUID userId,
            @Param("assetId") UUID assetId,
            Pageable pageable);
}
