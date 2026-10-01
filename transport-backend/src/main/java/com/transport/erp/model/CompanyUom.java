package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A unit of measure a company may use on material orders (booking, trip, invoice lines).
 * status ACTIVE = enabled for new order lines; INACTIVE = switched off (old lines keep showing it).
 * At most one enabled row per company is the default. See docs/UNITS_OF_MEASURE.md.
 */
@Getter
@Setter
@Entity
@Table(name = "company_uoms")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CompanyUom extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uom_id", nullable = false)
    private UomMaster uom;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
