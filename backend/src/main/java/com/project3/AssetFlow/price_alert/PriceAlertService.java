package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.identity.UserRepository;
import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.market.AssetRepository;
import com.project3.AssetFlow.price_alert.dto.AlertResponse;
import com.project3.AssetFlow.price_alert.dto.CreateAlertRequest;
import com.project3.AssetFlow.price_alert.dto.UpdateAlertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;

    public AlertResponse createAlert(Long userId, CreateAlertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Asset asset = assetRepository.findByTicker(request.ticker())
                .orElseThrow(() -> new IllegalStateException("Asset not found"));

        PriceAlert alert = new PriceAlert();
        alert.setAsset(asset);
        alert.setUser(user);
        alert.setTargetPrice(request.targetPrice());
        alert.setEnabled(true);
        priceAlertRepository.save(alert);

        return mapToResponse(alert);
    }

    @Transactional
    public AlertResponse updateAlert(Long userId, Long alertId, UpdateAlertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        PriceAlert alert = priceAlertRepository.findById(alertId)
                .filter(PriceAlert::isEnabled)
                .orElseThrow(() -> new IllegalStateException("Alert not found"));

        if(!alert.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("User does not own this alert");
        }

        if(request.newTargetPrice() == null || request.newTargetPrice().compareTo(alert.getTargetPrice()) == 0) {
            alert.setTargetPrice(request.newTargetPrice());
        }

        alert.setTargetPrice(request.newTargetPrice());
        priceAlertRepository.save(alert);

        return mapToResponse(alert);
    }

    public void deleteAlert(Long userId, Long alertId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        PriceAlert alert = priceAlertRepository.findById(alertId)
                .filter(PriceAlert::isEnabled)
                .orElseThrow(() -> new IllegalStateException("Alert not found"));

        if(!alert.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("User does not own this alert");
        }

        alert.setEnabled(false);
        priceAlertRepository.save(alert);
    }

    public List<AlertResponse> getAllAlerts(Long userId) {
        List<PriceAlert> alerts = priceAlertRepository.findByUserId(userId);

        return alerts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AlertResponse mapToResponse(PriceAlert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getUser().getId(),
                alert.getAsset().getId(),
                alert.getTargetPrice()
        );
    }
}
