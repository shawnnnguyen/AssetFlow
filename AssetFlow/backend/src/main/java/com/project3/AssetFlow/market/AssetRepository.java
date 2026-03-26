package com.project3.AssetFlow.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Asset findByTicker(String ticker);
}
