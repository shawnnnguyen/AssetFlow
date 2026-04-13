package com.project3.AssetFlow.currency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="exchange_rates")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="rate")
    private BigDecimal rate;

    @ManyToOne
    @JoinColumn(name="from_currency", referencedColumnName = "code")
    private Currency fromCurrency;

    @ManyToOne
    @JoinColumn(name="to_currency", referencedColumnName = "code")
    private Currency toCurrency;

    @Column(name="updated_at")
    private Instant updatedAt;
}
