package com.project3.AssetFlow.portfolio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    @Query("SELECT p FROM Portfolio p JOIN FETCH p.user JOIN FETCH p.currency WHERE p.user.id = :userId")
    List<Portfolio> findByUserId(UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portfolio p JOIN FETCH p.user WHERE p.id = :id")
    Optional<Portfolio> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT p FROM Portfolio p JOIN FETCH p.user JOIN FETCH p.currency WHERE p.id = :id")
    Optional<Portfolio> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT p FROM Portfolio p JOIN FETCH p.user WHERE p.id IN (SELECT h.portfolio.id FROM Holding h WHERE h.asset.id = :assetId)")
    List<Portfolio> findPortfoliosByAssetIdWithUser(UUID assetId);
}
