package com.project3.AssetFlow.transaction;

import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.portfolio.Portfolio;
import com.project3.AssetFlow.portfolio.TradeType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="transaction_id")
    private Long id;

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

    @Column(name="execution_time")
    private LocalDateTime executionTime;

    @Column(name="type")
    private TradeType type;

    public Transaction(Asset asset, BigDecimal quantity, BigDecimal pricePerUnit, LocalDateTime executionTime, TradeType type) {
        this.asset = asset;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.executionTime = executionTime;
        this.type = type;
    }
}
