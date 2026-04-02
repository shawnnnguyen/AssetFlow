package com.project3.AssetFlow.market;

import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset>  findByTicker(String ticker);

    @Query("SELECT new com.project3.AssetFlow.market.dto.TrackedStocksDTO(a.id, a.ticker, p.price) " +
            "FROM Asset a LEFT JOIN Price p ON p.asset = a " +
            "AND p.recordedAt = (SELECT MAX(p2.recordedAt) FROM Price p2 WHERE p2.asset = a)")
    List<TrackedStocksDTO> findAllTrackedAssetsWithLatestPrice();
}
