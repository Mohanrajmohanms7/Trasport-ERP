package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "materials")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Material extends BaseEntity {

    /** HSN / SAC code printed on invoices (V86, optional). */
    @Column(name = "hsn_code", length = 12)
    private String hsnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private LookupValue category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private LookupValue unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_uom_id")
    private UomMaster defaultUom;

    @Column(name = "default_rate", precision = 12, scale = 2)
    private BigDecimal defaultRate = BigDecimal.ZERO;

    @Column(precision = 8, scale = 3)
    private BigDecimal density = BigDecimal.ONE;
}
