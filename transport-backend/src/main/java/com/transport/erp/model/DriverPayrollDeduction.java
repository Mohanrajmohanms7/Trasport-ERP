package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fine / damage / other deduction on a payroll. Advance recovery is tracked in DriverAdvanceRecovery. */
@Getter
@Setter
@Entity
@Table(name = "driver_payroll_deductions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverPayrollDeduction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_id", nullable = false)
    @JsonBackReference("payroll-deductions")
    private DriverPayroll payroll;

    @Column(name = "deduction_type", nullable = false, length = 20)
    private String deductionType; // FINE, DAMAGE, OTHER

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "deduction_date", nullable = false)
    private LocalDate deductionDate;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
