package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DriverPayrollCreateDTO {
    private Long driverId;
    private Integer payYear;
    private Integer payMonth;
    private BigDecimal basicSalary;
    private BigDecimal allowanceAmount;
    private BigDecimal deductionAmount;
    private BigDecimal advanceAdjustment;
    private String description;
}
