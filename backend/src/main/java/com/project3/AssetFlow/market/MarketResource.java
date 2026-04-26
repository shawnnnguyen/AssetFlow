package com.project3.AssetFlow.market;

import com.project3.AssetFlow.market.dto.AssetProfileResponse;
import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketResource {

    private final MarketDataService service;

    @GetMapping("/profiles/{ticker}")
    public ResponseEntity<AssetProfileResponse> getCompanyProfile(@PathVariable String ticker) {
        AssetProfileResponse response = service.getCompanyProfile(ticker);

        if(response == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tracked-stocks")
    public ResponseEntity<List<TrackedStocksDTO>> getAllTrackedStocks() {
        List<TrackedStocksDTO> trackedStocks = service.getAllTrackedStocks();
        return ResponseEntity.ok(trackedStocks);
    }

    @PostMapping("/tracked-stocks/{ticker}")
    public ResponseEntity<String> addStockToTracking(@PathVariable String ticker) {
        EntityStatus result = service.addStockToTracking(ticker);

        return switch (result) {
            case ADDED -> ResponseEntity.status(HttpStatus.CREATED).body("Stock '" + ticker + "' added successfully.");
            case ALREADY_EXISTS -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: Stock '" + ticker + "' is already being tracked.");
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Stock '" + ticker + "' could not be found.");
            default -> throw new IllegalStateException("Unexpected TrackingResult value: " + result);
        };
    }

    @DeleteMapping("/tracked-stocks/{ticker}")
    public ResponseEntity<String> removeStockFromTracking(@PathVariable String ticker) {
        EntityStatus result = service.removeStockFromTracking(ticker);

        return switch (result) {
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Stock '" + ticker + "' could not be found.");
            case REMOVED -> ResponseEntity.ok("Stock '" + ticker + "' removed successfully.");
            case INVALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Invalid ticker provided.");
            default -> throw new IllegalStateException("Unexpected TrackingResult value: " + result);
        };
    }
}
