package com.project3.AssetFlow.market;

import com.project3.AssetFlow.currency.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="ticker", unique = true)
    private String ticker;

    @Column(name="asset_name")
    private String assetName;

    @Column(name="country")
    private String country;

    @Column(name="industry")
    private String industry;

    @ManyToOne
    @JoinColumn(name="currency_code", referencedColumnName = "code")
    private Currency currency;
}
