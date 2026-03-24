package com.project3.AssetFlow.market;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MarketResource {
    private MarketDataService service;

    public MarketResource(MarketDataService service) {
        this.service = service;
    }
}
