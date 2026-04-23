package com.project3.AssetFlow.price_alert;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="alert_triggered")
@Getter
@Setter
@NoArgsConstructor
public class AlertTriggered {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "price_alert_id")
    private PriceAlert priceAlert;

    @Column(name = "triggered_price")
    private BigDecimal triggeredPrice;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;
}
