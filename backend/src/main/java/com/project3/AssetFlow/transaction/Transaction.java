package com.project3.AssetFlow.transaction;

import com.project3.AssetFlow.currency.Currency;
import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.portfolio.Portfolio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_txn_user",             columnList = "user_id"),
    @Index(name = "idx_txn_portfolio",        columnList = "portfolio_id"),
    @Index(name = "idx_txn_asset",            columnList = "asset_id"),
    @Index(name = "idx_txn_portfolio_asset",  columnList = "portfolio_id, asset_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="portfolio_id")
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="asset_id")
    private Asset asset;

    @Column(name="quantity")
    private BigDecimal quantity;

    @Column(name="price_per_unit")
    private BigDecimal pricePerUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="currency_code", referencedColumnName = "code")
    private Currency currency;

    @Column(name="executed_at")
    private Instant executedAt;

    @Column(name="type")
    @Enumerated(EnumType.STRING)
    private TransactionType type;
}
