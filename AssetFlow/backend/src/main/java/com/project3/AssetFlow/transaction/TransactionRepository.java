package com.project3.AssetFlow.transaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Page<Transaction> findByPortfolioId(Long portfolioId, Pageable pageable);

    Page<Transaction> findByAssetIdAndPortfolioId(Long assetId,
                                                  Long portfolioId,
                                                  Pageable pageable);
}
