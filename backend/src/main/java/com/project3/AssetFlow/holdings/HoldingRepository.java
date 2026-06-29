package com.project3.AssetFlow.holdings;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holding h WHERE h.portfolio.id = :portfolioId AND h.asset.id = :assetId")
    Holding findByPortfolioIdAndAssetIdForUpdate(UUID portfolioId, UUID assetId);

    @Query("SELECT h FROM Holding h JOIN FETCH h.asset JOIN FETCH h.portfolio WHERE h.portfolio.id = :portfolioId")
    List<Holding> findByPortfolioIdWithDetails(@Param("portfolioId") UUID portfolioId);

    List<Holding> findByAssetId(UUID assetId);

    @Query("SELECT h FROM Holding h JOIN FETCH h.asset JOIN FETCH h.portfolio WHERE h.asset.ticker = :ticker AND h.portfolio.id = :portfolioId")
    List<Holding> findByTickerAndPortfolioId(@Param("ticker") String ticker, @Param("portfolioId") UUID portfolioId);
}
