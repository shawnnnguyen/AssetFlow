package com.project3.AssetFlow.market;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prices", indexes = {
    @Index(name = "idx_price_asset_recorded", columnList = "asset_id, recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Price {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="asset_id")
    private Asset asset;

    @Column(name="price")
    private BigDecimal price;

    @Column(name="recorded_at")
    private Instant recordedAt;
}
