package com.project3.AssetFlow.holdings;

import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.portfolio.Portfolio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "holdings", indexes = {
    @Index(name = "idx_holding_portfolio_asset", columnList = "portfolio_id, asset_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="portfolio_id")
    private Portfolio portfolio;

    @Column(name="avg_cost")
    private BigDecimal avgCost;

    @Column(name="quantity")
    private BigDecimal quantity;
}
