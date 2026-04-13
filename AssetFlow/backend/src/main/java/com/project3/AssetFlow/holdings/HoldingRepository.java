package com.project3.AssetFlow.holdings;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holding h WHERE h.portfolio.id = :portfolioId AND h.asset.id = :assetId")
    Holding findByPortfolioIdAndAssetIdForUpdate(Long portfolioId, Long assetId);

    List<Holding> findByPortfolioId(Long portfolioId);

    List<Holding> findByAssetId(Long assetId);

    @Query("SELECT h FROM Holding h WHERE h.asset.ticker = :ticker AND h.portfolio.id = :portfolioId")
    List<Holding> findByTickerAndPortfolioId(@Param("ticker") String ticker, @Param("portfolioId") Long portfolioId);
}