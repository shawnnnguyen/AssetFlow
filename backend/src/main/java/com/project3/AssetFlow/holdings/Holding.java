package com.project3.AssetFlow.holdings;

import com.project3.AssetFlow.market.Asset;
import com.project3.AssetFlow.portfolio.Portfolio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "holdings", indexes = {
    @Index(name = "idx_holding_portfolio_asset", columnList = "portfolio_id, asset_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Holding {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id")
    private UUID id;

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
