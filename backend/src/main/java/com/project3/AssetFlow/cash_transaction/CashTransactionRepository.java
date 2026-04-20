package com.project3.AssetFlow.cash_transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

    Page<CashTransaction> findByUserId(Long userId, Pageable pageable);

    Page<CashTransaction> findByPortfolioId(Long portfolioId, Pageable pageable);
}
