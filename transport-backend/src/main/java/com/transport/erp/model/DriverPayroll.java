package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "driver_payrolls")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverPayroll extends BaseEntity {

    @Column(name = "payroll_number", nullable = false, unique = true, length = 50)
    private String payrollNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "pay_year", nullable = false)
    private Integer payYear;

    @Column(name = "pay_month", nullable = false)
    private Integer payMonth;

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "allowance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal allowanceAmount = BigDecimal.ZERO;

    @Column(name = "deduction_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Column(name = "advance_adjustment", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceAdjustment = BigDecimal.ZERO;

    @Column(name = "net_salary_payable", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalaryPayable = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "accrual_jv_number", length = 100)
    private String accrualJvNumber;

    @Column(name = "payment_jv_number", length = 100)
    private String paymentJvNumber;

    @Column(name = "cancellation_jv_number", length = 100)
    private String cancellationJvNumber;
}
