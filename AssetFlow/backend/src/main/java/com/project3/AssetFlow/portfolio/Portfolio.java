package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.market.Currency;
import com.project3.AssetFlow.identity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name="portfolios")
@Data
@NoArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="portfolio_id")
    private Long portfolioId;

    @OneToOne
    @JoinColumn(name="user_id")
    private User user;

    @Column(name="cash_balance")
    private BigDecimal cashBalance;

    @ManyToOne
    @JoinColumn(name = "currency_preference")
    private Currency currency;

    @Transient
    private List<Asset> portfolio;

    public Portfolio(BigDecimal cashBalance, Currency currency) {
        this.cashBalance = cashBalance;
        this.currency = currency;
    }
}