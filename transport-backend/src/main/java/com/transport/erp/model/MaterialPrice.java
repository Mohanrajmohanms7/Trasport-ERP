package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "material_prices")
public class MaterialPrice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "material_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal materialRate = BigDecimal.ZERO;

    @Column(name = "transport_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal transportRate = BigDecimal.ZERO;

    @Column(name = "royalty_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal royaltyRate = BigDecimal.ZERO;

    @Column(name = "loading_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal loadingCharge = BigDecimal.ZERO;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
}
