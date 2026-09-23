package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderLabourResponse {
    private Long id;
    private String code;
    private Long appUserId;
    private String appUserName;
    private String description;
    private BigDecimal hours;
    private BigDecimal rate;
    private BigDecimal lineTotal;
    private LocalDate workDate;
    private String notes;
    private Integer version;
}
