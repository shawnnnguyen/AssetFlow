package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.price_alert.dto.AlertTriggeredResponse;
import com.project3.AssetFlow.streaming.events.PriceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerAlertService {

    private final PriceAlertRepository alertRepository;
    private final AlertTriggeredRepository alertTriggeredRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("alertNotificationExecutor")
    @EventListener
    @Transactional
    public void handleAssetPriceUpdate(PriceUpdateEvent event) {

        try {
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
        } catch (Exception e) {
            log.error("Failed to process price alerts for asset ID: {} at price {}. Notifications were not sent.",
                    event.assetId(), event.latestPrice(), e);
        }
    }

    public void dispatchAlertNotifications(List<AlertTriggered> alerts) {
        List<AlertTriggered> successfullyDispatched = new ArrayList<>();

        for (AlertTriggered alert : alerts) {
            try {
                PriceAlert alertToNotify = alert.getPriceAlert();
                Long targetUserId = alertToNotify.getUser().getId();

                AlertTriggeredResponse payload = new AlertTriggeredResponse(
                        alert.getId(),
                        alertToNotify.getId(),
                        targetUserId,
                        alertToNotify.getAsset().getId(),
                        alert.getTriggeredPrice(),
                        alert.getTriggeredAt()
                );

                messagingTemplate.convertAndSend("/topic/alerts/" + targetUserId, payload);
                successfullyDispatched.add(alert);
            } catch (Exception e) {
                log.error("Error dispatching alert notification", e);
            }
        }
        if (!successfullyDispatched.isEmpty()) {
            alertTriggeredRepo.saveAll(successfullyDispatched);
        }
    }
}
