package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class DriverPayrollPrintDTO {

    // Payroll Information
    private Long payrollId;
    private String payrollNumber;
    private Integer payYear;
    private Integer payMonth;
    private String payPeriod;
    private String status;
    private String paymentMethod;
    private LocalDateTime createdDate;

    // Driver Information
    private Long driverId;
    private String driverName;
    private String driverCode;
    private String driverPhone;
    private String licenseNumber;

    // Financial Components
    private BigDecimal basicSalary = BigDecimal.ZERO;
    private BigDecimal allowanceAmount = BigDecimal.ZERO;
    private BigDecimal grossEarnings = BigDecimal.ZERO;

    private BigDecimal deductionAmount = BigDecimal.ZERO;
    private BigDecimal advanceAdjustment = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    private BigDecimal netSalaryPayable = BigDecimal.ZERO;

    // Accounting JV References
    private String accrualJvNumber;
    private String paymentJvNumber;
    private String cancellationJvNumber;

    // Company & Branch Information
    private Long companyId;
    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    private String companyGSTIN;

    private Long branchId;
    private String branchName;
    private String branchAddress;
}
