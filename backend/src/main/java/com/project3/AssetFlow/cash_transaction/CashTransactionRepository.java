package com.project3.AssetFlow.cash_transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, UUID> {

    Page<CashTransaction> findByUserId(UUID userId, Pageable pageable);

    Page<CashTransaction> findByPortfolioId(UUID portfolioId, Pageable pageable);
}
