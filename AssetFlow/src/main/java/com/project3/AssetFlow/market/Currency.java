package com.project3.AssetFlow.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="currencies")
public class Currency {
    @Id
    @Column(name="code", unique = true)
    private String code;

    @Column(name="symbol")
    private String symbol;
}
