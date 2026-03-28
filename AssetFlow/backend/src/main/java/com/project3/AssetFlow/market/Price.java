package com.project3.AssetFlow.market;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="prices")
@Data
@NoArgsConstructor
public class Price {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="asset_id")
    private Asset asset;

    @ManyToOne
    @JoinColumn(name="currency_code")
    private Currency currency;

    @Column(name="price")
    private BigDecimal price;

    @Column(name="recorded_at")
    private Instant recordedAt;

    public Price(Asset asset, Currency currency, BigDecimal price, Instant recordedAt) {
        this.asset = asset;
        this.currency = currency;
        this.price = price;
        this.recordedAt = recordedAt;
    }
}
