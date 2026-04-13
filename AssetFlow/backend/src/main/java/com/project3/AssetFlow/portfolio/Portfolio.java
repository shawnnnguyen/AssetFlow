package com.project3.AssetFlow.portfolio;

import com.project3.AssetFlow.identity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="portfolios")
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name="name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name="status")
    private PortfolioStatusType status;

    @Column(name="cash_balance")
    private BigDecimal cashBalance;

    @Column(name = "currency_preference")
    private String currency;

    @Column(name="created_at")
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;
}