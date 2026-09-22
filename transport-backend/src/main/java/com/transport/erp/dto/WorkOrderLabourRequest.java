package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderLabourRequest {
    private Long appUserId;
    private String description;
    private BigDecimal hours;
    private BigDecimal rate;
    private LocalDate workDate;
    private String notes;
}
