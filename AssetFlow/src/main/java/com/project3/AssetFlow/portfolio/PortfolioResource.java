package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.transaction.Transaction;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
public class PortfolioResource {
    @Autowired
    private final PortfolioService portfolioService;
}
