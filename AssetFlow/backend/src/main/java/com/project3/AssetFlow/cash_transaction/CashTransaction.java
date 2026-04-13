package com.project3.AssetFlow.cash_transaction;

import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.portfolio.Portfolio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="cash_transactions")
@Getter
@Setter
@NoArgsConstructor
public class CashTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name="portfolio_id")
    private Portfolio portfolio;

    @Column(name="type")
    private CashTransactionType type;

    @Column(name="amount")
    private BigDecimal amount;

    @Column(name="executed_at")
    private Instant executedAt;
}
