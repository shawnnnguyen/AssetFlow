package com.project3.AssetFlow.market;

import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MarketResource {
    private final MarketDataService service;

    public MarketResource(MarketDataService service) {
        this.service = service;
    }

    @GetMapping(value="/market/search/{ticker}")
    public ResponseEntity<String> getCompanyProfile (@PathVariable String ticker) {

        TrackingResult result = service.getCompanyProfile(ticker);

        return switch (result) {
            case FOUND -> ResponseEntity.ok("Profile of '" + ticker + "' found.");
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Profile of '" + ticker + "' could not be found.");
            case ALREADY_TRACKED -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: Profile of '" + ticker + "' is already being tracked.");

            default -> throw new IllegalStateException("Unexpected TrackingResult value: " + result);
        };
    }

    @PostMapping(value="/market/track/{ticker}")
    public ResponseEntity<String> addStockToTracking(@PathVariable String ticker) {

        TrackingResult result = service.addStockToTracking(ticker);

        return switch (result) {
            case ADDED -> ResponseEntity.ok("Stock '" + ticker + "' added successfully.");
            case ALREADY_TRACKED -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error: Stock '" + ticker + "' is already being tracked.");
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Stock '" + ticker + "' could not be found.");

            default -> throw new IllegalStateException("Unexpected TrackingResult value: " + result);
        };
    }

    @DeleteMapping(value="/market/untrack/{ticker}")
    public ResponseEntity<String> removeStockFromTracking(@PathVariable String ticker) {
        TrackingResult result = service.removeStockFromTracking(ticker);

        return switch (result) {
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Stock '" + ticker + "' could not be found.");
            case NOT_TRACKED -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Stock '" + ticker + "' is ");
            case ADDED -> ResponseEntity.ok("Stock '" + ticker + "' removed successfully.");

            default -> throw new IllegalStateException("Unexpected TrackingResult value: " + result);
        };
    }

    @GetMapping(value="/market")
    public ResponseEntity<List<TrackedStocksDTO>> getAllTrackedStocks() {
        List<TrackedStocksDTO> trackedStocks = service.getAllTrackedStocks();
        return ResponseEntity.ok(trackedStocks);
    }
}
