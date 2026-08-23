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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private LookupValue category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private LookupValue unit;

    @Column(name = "default_rate", precision = 12, scale = 2)
    private BigDecimal defaultRate = BigDecimal.ZERO;

    @Column(precision = 8, scale = 3)
    private BigDecimal density = BigDecimal.ONE;
}
