package com.project3.AssetFlow.market;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="prices")
@Getter
@Setter
@NoArgsConstructor
public class Price {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="asset_id")
    private Asset asset;

    @Column(name="price")
    private BigDecimal price;

    @Column(name="recorded_at")
    private Instant recordedAt;
}
