package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.price_alert.dto.AlertTriggeredResponse;
import com.project3.AssetFlow.streaming.events.PriceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerAlertService {

    private final PriceAlertRepository alertRepository;
    private final AlertTriggeredRepository alertTriggeredRepo;
    private final SimpMessagingTemplate messagingTemplate;

    private final Queue<AlertTriggeredResponse  > pendingNotifications = new ConcurrentLinkedQueue<>();

    @Async
    @EventListener
    @Transactional(readOnly = true)
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

                alertsToSave.add(alertTriggered);
            }

            List<AlertTriggered> savedAlerts = alertTriggeredRepo.saveAll(alertsToSave);

            for (AlertTriggered alert : savedAlerts) {
                AlertTriggeredResponse response = new AlertTriggeredResponse(
                        alert.getId(),
                        alert.getPriceAlert().getId(),
                        alert.getPriceAlert().getUser().getId(),
                        alert.getPriceAlert().getAsset().getId(),
                        alert.getTriggeredPrice(),
                        alert.getTriggeredAt()
                );

                pendingNotifications.offer(response);
            }
        } catch (Exception e) {
            log.error("Error calculating portfolio performance", e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void dispatchPendingNotifications() {
        if (pendingNotifications.isEmpty()) {
            return;
        }

        log.info("Dispatching {} pending price alerts via WebSocket...", pendingNotifications.size());

        while (!pendingNotifications.isEmpty()) {
            AlertTriggeredResponse payload = pendingNotifications.poll();

            if (payload != null) {
                try {
                    PriceAlert alert = alertRepository.findById(payload.alertId())
                            .orElseThrow();
                    Long targetUserId = alert.getUser().getId();

                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(targetUserId),
                            "/queue/alerts",
                            payload
                    );
                } catch (Exception e) {
                    log.error("Failed to send websocket alert history ID: {}", payload.alertTriggeredId(), e);
                }
            }
        }
    }
}
