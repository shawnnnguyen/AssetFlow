package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.currency.Currency;
import com.project3.AssetFlow.currency.CurrencyConversionService;
import com.project3.AssetFlow.currency.CurrencyRepository;
import com.project3.AssetFlow.holdings.Holding;
import com.project3.AssetFlow.holdings.HoldingRepository;
import com.project3.AssetFlow.holdings.HoldingService;
import com.project3.AssetFlow.holdings.dto.HoldingPerformance;
import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.identity.UserRepository;
import com.project3.AssetFlow.market.MarketDataService;
import com.project3.AssetFlow.market.dto.TrackedStocksDTO;
import com.project3.AssetFlow.portfolio.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final HoldingService holdingService;
    private final MarketDataService marketDataService;
    private final CurrencyConversionService currencyConversionService;
    private final CurrencyRepository currencyRepository;

    public List<PortfolioResponse> getAllPortfoliosByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId).stream()
                .map(this::mapToPortfolioResponse)
                .toList();
    }

    public PortfolioResponse getPortfolioById(Long userId, Long portfolioId) {
        return mapToPortfolioResponse(getVerifiedPortfolio(userId, portfolioId));
    }

    @Transactional
    public NewPortfolioResponse addNewPortfolio(NewPortfolioRequest requestedPortfolio, Long userId) {
        if (portfolioRepository.existsByUserIdAndNameIgnoreCase(userId, requestedPortfolio.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Portfolio with this name already exists");
        }

        User userProxy = userRepository.getReferenceById(userId);
        Portfolio newPortfolio = new Portfolio();

        Currency baseCurrency = currencyRepository.findByCode(requestedPortfolio.currencyCode());
        if (baseCurrency == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found: " + requestedPortfolio.currencyCode());

        newPortfolio.setUser(userProxy);
        newPortfolio.setName(requestedPortfolio.name());
        newPortfolio.setCurrency(baseCurrency);
        newPortfolio.setCashBalance(BigDecimal.ZERO);
        newPortfolio.setCreatedAt(Instant.now());
        newPortfolio.setUpdatedAt(Instant.now());
        newPortfolio.setStatus(PortfolioStatusType.ACTIVE);

        Portfolio savedPortfolio = portfolioRepository.save(newPortfolio);

        return mapToNewPortfolioResponse(savedPortfolio);
    }

    @Transactional
    public PortfolioResponse updateVerifiedPortfolio(UpdatePortfolioRequest requestedPortfolio,
                                                     Long userId,
                                                     Long portfolioId) {
        Portfolio portfolio = getVerifiedPortfolio(userId, portfolioId);
        String portfolioCurrencyCode = portfolio.getCurrency().getCode();

        boolean isChanged = false;

        if (requestedPortfolio.name() != null && !requestedPortfolio.name().equals(portfolio.getName())) {
            portfolio.setName(requestedPortfolio.name());
            isChanged = true;
        }
        if (requestedPortfolio.currencyCode() != null && !requestedPortfolio.currencyCode().equals(portfolioCurrencyCode)) {
            Currency newBaseCurrency = currencyRepository.findByCode(requestedPortfolio.currencyCode());

            if (newBaseCurrency == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found: " + requestedPortfolio.currencyCode());
            BigDecimal convertedCashBalance = currencyConversionService.convertCurrency(
                    portfolioCurrencyCode, requestedPortfolio.currencyCode(), portfolio.getCashBalance());

            portfolio.setCurrency(newBaseCurrency);
            portfolio.setCashBalance(convertedCashBalance);
            isChanged = true;
        }

        if (isChanged) {
            portfolio.setUpdatedAt(Instant.now());
            portfolioRepository.save(portfolio);
        }

        return mapToPortfolioResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse closePortfolio(Long userId, Long portfolioId) {
        Portfolio portfolio = getVerifiedPortfolio(userId, portfolioId);
        portfolio.setStatus(PortfolioStatusType.CLOSED);
        portfolio.setUpdatedAt(Instant.now());

        return mapToPortfolioResponse(portfolio);
    }

    public PortfolioPerformanceResponse calculatePortfolioPerformance(Portfolio portfolio,
                                                                      List<Holding> holdings,
                                                                      Map<Long, BigDecimal> livePrices) {
        if (holdings.isEmpty()) {
            return new PortfolioPerformanceResponse(
                    portfolio.getId(),
                    BigDecimal.ZERO,
                    portfolio.getCashBalance(),
                    portfolio.getCashBalance(),
                    new PerformanceMetrics(BigDecimal.ZERO, BigDecimal.ZERO),
                    List.of()
            );
        }

        List<HoldingPerformance> holdingPerformances = holdings.stream()
                .map(holding -> {
                    BigDecimal livePrice = livePrices.getOrDefault(holding.getAsset().getId(), BigDecimal.ZERO);
                    return holdingService.calculateHoldingPerformance(holding, livePrice);
                })
                .toList();

        BigDecimal totalInvestment = holdings.stream()
                .map(h -> h.getQuantity().multiply(h.getAvgCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalHoldingsValue = holdingPerformances.stream()
                .map(HoldingPerformance::currentTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal portfolioValue = totalHoldingsValue.add(portfolio.getCashBalance());
        BigDecimal absoluteChange = totalHoldingsValue.subtract(totalInvestment);

        BigDecimal percentageChange = BigDecimal.ZERO;

        if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = absoluteChange
                    .divide(totalInvestment, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new PortfolioPerformanceResponse(
                portfolio.getId(),
                totalInvestment,
                portfolioValue,
                portfolio.getCashBalance(),
                new PerformanceMetrics(absoluteChange, percentageChange),
                holdingPerformances
        );
    }

    public PortfolioPerformanceResponse getPortfolioPerformance(Long userId, Long portfolioId) {
        Portfolio portfolio = getVerifiedPortfolio(userId, portfolioId);
        List<Holding> rawHoldings = holdingRepository.findByPortfolioIdWithDetails(portfolioId);

        Map<Long, BigDecimal> livePrices = marketDataService.getAllTrackedStocks()
                .stream()
                .filter(s -> s.latestPrice() != null)
                .collect(Collectors.toMap(
                        TrackedStocksDTO::assetId,
                        TrackedStocksDTO::latestPrice,
                        (existing, replacement) -> existing
                ));

        return calculatePortfolioPerformance(portfolio, rawHoldings, livePrices);
    }

    public Portfolio getVerifiedPortfolio(Long userId, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));

        if (!portfolio.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Portfolio does not belong to the user");
        }
        return portfolio;
    }

    private NewPortfolioResponse mapToNewPortfolioResponse(Portfolio portfolio) {
        return new NewPortfolioResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getStatus(),
                portfolio.getCurrency().getCode(),
                portfolio.getCashBalance());
    }

    private PortfolioResponse mapToPortfolioResponse(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getCurrency().getCode(),
                portfolio.getCashBalance(),
                portfolio.getStatus() != null ? portfolio.getStatus().name() : "ACTIVE",
                portfolio.getCreatedAt()
        );
    }
}
