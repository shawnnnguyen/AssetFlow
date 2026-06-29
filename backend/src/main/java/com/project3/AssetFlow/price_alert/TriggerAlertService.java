package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.price_alert.dto.AlertTriggeredResponse;
import com.project3.AssetFlow.streaming.events.PriceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerAlertService {

    private final PriceAlertRepository alertRepository;
    private final AlertTriggeredRepository alertTriggeredRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("alertNotificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAssetPriceUpdate(PriceUpdateEvent event) {

        List<PriceAlert> triggeredAlerts = alertRepository.findTriggeredAlerts(
                event.assetId(),
                event.oldPrice(),
                event.latestPrice()
        );

        if (triggeredAlerts.isEmpty()) return;

        List<AlertTriggered> alertsToSave = new ArrayList<>();

        for (PriceAlert alert : triggeredAlerts) {
            AlertTriggered alertTriggered = new AlertTriggered();
            alertTriggered.setPriceAlert(alert);
            alertTriggered.setTriggeredPrice(event.latestPrice());
            alertTriggered.setTriggeredAt(Instant.now());

            alert.setEnabled(false);
            alertsToSave.add(alertTriggered);
        }

        List<AlertTriggered> savedAlerts = alertTriggeredRepo.saveAll(alertsToSave);

        dispatchAlertNotifications(savedAlerts);
    }

    public void dispatchAlertNotifications(List<AlertTriggered> alerts) {
        List<AlertTriggered> successfullyDispatched = new ArrayList<>();

        for (AlertTriggered alert : alerts) {
            try {
                PriceAlert alertToNotify = alert.getPriceAlert();
                UUID targetUserId = alertToNotify.getUser().getId();

                AlertTriggeredResponse payload = new AlertTriggeredResponse(
                        alert.getId(),
                        alertToNotify.getId(),
                        targetUserId,
                        alertToNotify.getAsset().getId(),
                        alert.getTriggeredPrice(),
                        alert.getTriggeredAt()
                );

                messagingTemplate.convertAndSend("/topic/alerts/" + targetUserId, payload);
                alert.setDispatchedAt(Instant.now());
                successfullyDispatched.add(alert);
            } catch (Exception e) {
                log.error("Error dispatching alert notification", e);
            }
        }
    }
}
