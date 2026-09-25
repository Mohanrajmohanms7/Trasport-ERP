package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Input to generate or edit a DRAFT payroll. Trip counts, daily pay, gross and net are always
 * calculated on the server; only the amounts an accountant decides are accepted here.
 */
@Getter
@Setter
public class DriverPayrollCreateDTO {
    private Long driverId;
    private Integer payYear;
    private Integer payMonth;
    /** Ignored: basic salary comes from the driver's salary configuration. Kept for API compatibility. */
    private BigDecimal basicSalary;
    private BigDecimal allowanceAmount;
    /** Ignored when deductions[] is sent. Legacy single deduction amount (type OTHER). */
    private BigDecimal deductionAmount;
    private BigDecimal advanceAdjustment;
    private String description;
    private List<Deduction> deductions = new ArrayList<>();

    @Getter
    @Setter
    public static class Deduction {
        private String deductionType; // FINE, DAMAGE, OTHER
        private BigDecimal amount;
        private LocalDate deductionDate;
        private String remarks;
    }
}
