package com.project3.AssetFlow.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<Price, Long> {
    @Query(value = """
           SELECT * FROM prices
           WHERE asset_id = :assetId 
           AND recorded_at <= :executedAt 
           ORDER BY recorded_at DESC 
           LIMIT 1
           """, nativeQuery = true)
    Optional<Price> findPriceAsOf(
            @Param("assetId") Long assetId,
            @Param("executedAt") Instant executedAt
    );
}