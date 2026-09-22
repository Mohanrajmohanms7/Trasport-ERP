package com.transport.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "spare_parts")
public class SparePart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "default_uom_id", nullable = false)
    private UomMaster defaultUom;

    @Column(name = "default_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal defaultRate = BigDecimal.ZERO;
}
