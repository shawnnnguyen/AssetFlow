package com.project3.AssetFlow.market;

import com.project3.AssetFlow.portfolio.AssetType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
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

    @Column(name="asset_type")
    private AssetType assetType;

    public Asset(String ticker, String assetName,String country, AssetType assetType) {
        this.ticker = ticker;
        this.assetName = assetName;
        this.country = country;
        this.assetType = assetType;
    }
}
