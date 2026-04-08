package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.market.Asset;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name="holdings")
@Getter
@Setter
@NoArgsConstructor
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="holding_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="asset_id")
    private Asset asset;

    @ManyToOne
    @JoinColumn(name="portfolio_id")
    private Portfolio portfolio;

    @Column(name="avg_cost")
    private BigDecimal avgCost;

    @Column(name="quantity")
    private BigDecimal quantity;

    public Holding(Asset asset, Portfolio portfolio, BigDecimal avgCost, BigDecimal quantity) {
        this.asset = asset;
        this.portfolio = portfolio;
        this.avgCost = avgCost;
        this.quantity = quantity;
    }
}
