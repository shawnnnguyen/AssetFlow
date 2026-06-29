package com.project3.AssetFlow.price_alert;

import com.project3.AssetFlow.identity.User;
import com.project3.AssetFlow.market.Asset;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "price_alerts", indexes = {
    @Index(name = "idx_alert_user",         columnList = "user_id"),
    @Index(name = "idx_alert_asset",        columnList = "asset_id"),
    @Index(name = "idx_alert_asset_enabled", columnList = "asset_id, enabled")
})
@Getter
@Setter
@NoArgsConstructor
public class PriceAlert {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "target_price")
    private BigDecimal targetPrice;
}
