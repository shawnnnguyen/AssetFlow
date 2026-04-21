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
        priceAlertRepository.save(alert);

        return new AlertResponse(
                alert.getId(),
                alert.getUser().getId(),
                asset.getId(),
                request.targetPrice()
        );
    }

    @Transactional
    public AlertResponse updateAlert(Long userId, Long alertId, UpdateAlertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalStateException("Alert not found"));

        if(!alert.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("User does not own this alert");
        }

        if(request.newTargetPrice() == null || request.newTargetPrice().equals(alert.getTargetPrice())) {
            return null;
        }

        alert.setTargetPrice(request.newTargetPrice());
        priceAlertRepository.save(alert);

        return new AlertResponse(
                alert.getId(),
                alert.getUser().getId(),
                alert.getAsset().getId(),
                request.newTargetPrice()
        );
    }

    public void deleteAlert(Long userId, Long alertId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalStateException("Alert not found"));

        if(!alert.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("User does not own this alert");
        }

        priceAlertRepository.delete(alert);
    }

    public List<AlertResponse> getAllAlerts(Long userId) {
        List<PriceAlert> alerts = priceAlertRepository.findByUserId(userId);

        return alerts.stream()
                .map(alert -> new AlertResponse(
                        alert.getId(),
                        alert.getUser().getId(),
                        alert.getAsset().getId(),
                        alert.getTargetPrice()
                ))
                .toList();
    }
}
