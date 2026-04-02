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

        boolean isFound = service.getCompanyProfile(ticker);

        if(!isFound) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Error: The ticker '" + ticker + "' could not be found.");
        }

        return ResponseEntity.ok("Ticker '" + ticker + "' found.");
    }

    @PostMapping(value="/market/track/{ticker}")
    public ResponseEntity<String> addStockToTracking(@PathVariable String ticker) {

        boolean isSuccess = service.addStockToTracking(ticker);

        if(!isSuccess) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Error: The ticker '" + ticker + "' is already being tracked.");
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Ticker '" + ticker + "' is now being tracked.");
    }

    @DeleteMapping(value="/market/untrack/{ticker}")
    public ResponseEntity<String> removeStockFromTracking(@PathVariable String ticker) {
        boolean isRemove = service.removeStockFromTracking(ticker);

        if(!isRemove) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Error: The ticker '" + ticker + "' could not be found.");
        }

        return ResponseEntity.ok("Ticker '" + ticker + "' removed successfully.");
    }

    @GetMapping(value="/market")
    public ResponseEntity<List<TrackedStocksDTO>> getAllTrackedStocks() {
        List<TrackedStocksDTO> trackedStocks = service.getAllTrackedStocks();
        return ResponseEntity.ok(trackedStocks);
    }
}
