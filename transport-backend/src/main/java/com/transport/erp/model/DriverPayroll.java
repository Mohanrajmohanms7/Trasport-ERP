package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "driver_payrolls")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverPayroll extends BaseEntity {

    @Column(name = "payroll_number", nullable = false, length = 50)
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

    /** Completed trips counted for the month. */
    @Column(name = "total_trips", nullable = false)
    private Integer totalTrips = 0;

    /** Days with at least one completed trip. */
    @Column(name = "trip_days", nullable = false)
    private Integer tripDays = 0;

    /** Sum of daily slab amounts. */
    @Column(name = "trip_earnings", nullable = false, precision = 12, scale = 2)
    private BigDecimal tripEarnings = BigDecimal.ZERO;

    /** trip earnings + basic salary + allowance. Posted as salary expense. */
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "recovery_jv_number", length = 100)
    private String recoveryJvNumber;

    @Column(name = "deduction_jv_number", length = 100)
    private String deductionJvNumber;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "paid_by", length = 100)
    private String paidBy;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("workDate ASC")
    @JsonManagedReference("payroll-days")
    private List<DriverPayrollDay> days = new ArrayList<>();

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id ASC")
    @JsonManagedReference("payroll-deductions")
    private List<DriverPayrollDeduction> deductions = new ArrayList<>();
}
