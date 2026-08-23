package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "branches")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Branch extends BaseEntity {

    /** Lazy company link — ignore on getter too (Jackson uses getters, not fields). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    @Getter(onMethod_ = {@JsonIgnore})
    @Setter
    private Company company;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(length = 100)
    private String manager;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
}
