package com.project3.AssetFlow.price_alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    @Query(value = """
        SELECT * FROM price_alerts
        WHERE asset_id = :assetId
        AND ((:oldPrice <= targer_price AND :newPrice >= target_price)
            OR (:oldPrice >= targer_price AND :newPrice <= target_price))
""", nativeQuery = true)
    List<PriceAlert> findTriggeredAlerts(Long assetId, BigDecimal oldPrice, BigDecimal latestPrice);

    List<PriceAlert> findByUserId(Long userId);
}
