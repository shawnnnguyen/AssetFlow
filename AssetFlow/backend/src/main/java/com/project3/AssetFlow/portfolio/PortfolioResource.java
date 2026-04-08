package com.project3.AssetFlow.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PortfolioResource {

    private final PortfolioService portfolioService;
}
