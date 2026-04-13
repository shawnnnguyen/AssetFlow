package com.project3.AssetFlow.transaction;

import com.project3.AssetFlow.currency.Currency;
import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.portfolio.Portfolio;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name="portfolio_id")
    private Portfolio portfolio;

    @ManyToOne
    @JoinColumn(name="asset_id")
    private Asset asset;

    @Column(name="quantity")
    private BigDecimal quantity;

    @Column(name="price_per_unit")
    private BigDecimal pricePerUnit;

    @ManyToOne
    @JoinColumn(name="currency_code", referencedColumnName = "code")
    private Currency currency;

    @Column(name="executed_at")
    private Instant executedAt;

    @Column(name="type")
    private TransactionType type;
}
