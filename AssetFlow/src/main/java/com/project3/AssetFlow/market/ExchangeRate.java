package com.project3.AssetFlow.market;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@Table(name="exchange_rates")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="rate")
    private BigDecimal rate;

    @ManyToOne
    @JoinColumn(name="from_currency_id")
    private Currency fromCurrencyId;

    @ManyToOne
    @JoinColumn(name="to_currency_id")
    private Currency toCurrencyId;

    @Column(name="recorded_at")
    private LocalDate recordedAt;

    public ExchangeRate(BigDecimal rate, LocalDate recordedAt) {
        this.rate = rate;
        this.recordedAt = recordedAt;
    }
}
