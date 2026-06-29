package com.project3.AssetFlow.price_alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertTriggeredRepository extends JpaRepository<AlertTriggered, UUID> {
}
