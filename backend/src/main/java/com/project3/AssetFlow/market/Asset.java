package com.project3.AssetFlow.market;

import com.project3.AssetFlow.currency.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="assets")
public class Asset {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name="ticker", unique = true)
    private String ticker;

    @Column(name="asset_name")
    private String assetName;

    @Column(name="country")
    private String country;

    @Column(name="industry")
    private String industry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="currency_code", referencedColumnName = "code")
    private Currency currency;
}
