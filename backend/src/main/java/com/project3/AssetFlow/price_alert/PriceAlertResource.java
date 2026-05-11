package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.identity.securityConfig.UserPrincipal;
import com.project3.AssetFlow.price_alert.dto.AlertResponse;
import com.project3.AssetFlow.price_alert.dto.CreateAlertRequest;
import com.project3.AssetFlow.price_alert.dto.UpdateAlertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/price-alerts")
@RequiredArgsConstructor
public class PriceAlertResource {

    private final PriceAlertService priceAlertService;

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(priceAlertService.createAlert(principal.getId(), request));
    }

    @PatchMapping("/{alertId}")
    public ResponseEntity<AlertResponse> updateAlert(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long alertId,
            @Valid @RequestBody UpdateAlertRequest request) {
        return ResponseEntity.ok(priceAlertService.updateAlert(principal.getId(), alertId, request));
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAllAlerts(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(priceAlertService.getAllAlerts(principal.getId()));
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> deleteAlert(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long alertId) {
        priceAlertService.deleteAlert(principal.getId(), alertId);
        return ResponseEntity.noContent().build();
    }
}
