package com.project3.AssetFlow.price_alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    @Query("""
        SELECT pa FROM PriceAlert pa 
        JOIN FETCH pa.user 
        JOIN FETCH pa.asset 
        WHERE pa.asset.id = :assetId 
        AND pa.enabled = true 
        AND (
            (:oldPrice <= pa.targetPrice AND :latestPrice >= pa.targetPrice)
            OR 
            (:oldPrice >= pa.targetPrice AND :latestPrice <= pa.targetPrice)
        )
    """)
    List<PriceAlert> findTriggeredAlerts(
            @Param("assetId") Long assetId,
            @Param("oldPrice") BigDecimal oldPrice,
            @Param("latestPrice") BigDecimal latestPrice
    );

    List<PriceAlert> findByUserId(Long userId);
}
