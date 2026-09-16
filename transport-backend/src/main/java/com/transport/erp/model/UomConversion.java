package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "uom_conversions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UomConversion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_uom_id", nullable = false)
    private UomMaster fromUom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_uom_id", nullable = false)
    private UomMaster toUom;

    @Column(name = "conversion_factor", nullable = false, precision = 14, scale = 6)
    private BigDecimal conversionFactor;
}
