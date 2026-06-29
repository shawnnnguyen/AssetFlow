package com.project3.AssetFlow.price_alert;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="alert_triggered")
@Getter
@Setter
@NoArgsConstructor
public class AlertTriggered {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_alert_id")
    private PriceAlert priceAlert;

    @Column(name = "triggered_price")
    private BigDecimal triggeredPrice;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;
}
