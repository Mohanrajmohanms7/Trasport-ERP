package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Portion of an advance recovered by a posted payroll. Status ACTIVE or REVERSED. */
@Getter
@Setter
@Entity
@Table(name = "driver_advance_recoveries")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverAdvanceRecovery extends BaseEntity {

    @Column(name = "payroll_id", nullable = false)
    private Long payrollId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "advance_id", nullable = false)
    private DriverAdvance advance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
}
