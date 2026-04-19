package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.market.Asset;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "price_alerts")
@Getter
@Setter
@NoArgsConstructor
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "target_price")
    private BigDecimal targetPrice;

    @OneToMany(mappedBy = "priceAlert", cascade = CascadeType.ALL)
    private List<AlertTriggered> history;
}
