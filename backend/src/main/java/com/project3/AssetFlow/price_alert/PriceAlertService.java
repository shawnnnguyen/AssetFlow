package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.identity.UserRepository;
import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.market.AssetRepository;
import com.project3.AssetFlow.price_alert.dto.AlertResponse;
import com.project3.AssetFlow.price_alert.dto.CreateAlertRequest;
import com.project3.AssetFlow.price_alert.dto.UpdateAlertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;

    @Transactional
    public AlertResponse createAlert(Long userId, CreateAlertRequest request) {
        Asset asset = assetRepository.findByTicker(request.ticker())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

        User userRef = userRepository.getReferenceById(userId);

        PriceAlert alert = new PriceAlert();
        alert.setAsset(asset);
        alert.setUser(userRef);
        alert.setTargetPrice(request.targetPrice());
        alert.setEnabled(true);
        priceAlertRepository.save(alert);

        return mapToResponse(alert);
    }

    @Transactional
    public AlertResponse updateAlert(Long userId, Long alertId, UpdateAlertRequest request) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .filter(PriceAlert::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (!alert.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found");
        }

        alert.setTargetPrice(request.newTargetPrice());

        return mapToResponse(alert);
    }

    @Transactional
    public void deleteAlert(Long userId, Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .filter(PriceAlert::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (!alert.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found");
        }

        alert.setEnabled(false);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts(Long userId) {
        return priceAlertRepository.findEnabledByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AlertResponse mapToResponse(PriceAlert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getUser().getId(),
                alert.getAsset().getId(),
                alert.getAsset().getTicker(),
                alert.getTargetPrice()
        );
    }
}
