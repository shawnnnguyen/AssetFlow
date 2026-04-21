package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.price_alert.dto.AlertResponse;
import com.project3.AssetFlow.price_alert.dto.CreateAlertRequest;
import com.project3.AssetFlow.price_alert.dto.UpdateAlertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/price-alerts")
@RequiredArgsConstructor
public class PriceAlertResource {

    private final PriceAlertService priceAlertService;

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(@PathVariable Long userId,
                                                     @Valid @RequestBody CreateAlertRequest request) {
        return ResponseEntity.status(201).body(priceAlertService.createAlert(userId, request));
    }

    @PatchMapping(value = "/{alertId}")
    public ResponseEntity<AlertResponse> updateAlert(@PathVariable Long userId,
                                                              @PathVariable Long alertId,
                                                              @Valid @RequestBody UpdateAlertRequest request) {
        return ResponseEntity.ok(priceAlertService.updateAlert(userId, alertId, request));
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAllAlerts(@PathVariable Long userId) {
        return ResponseEntity.ok(priceAlertService.getAllAlerts(userId));
    }

    @DeleteMapping(value = "/{alertId}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long userId, @PathVariable Long alertId) {
        priceAlertService.deleteAlert(userId, alertId);
        return ResponseEntity.noContent().build();
    }
}
