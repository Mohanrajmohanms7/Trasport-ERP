package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class MaintenanceRuleResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private Long companyId;
    private Long branchId;
    private String maintenanceType;
    private String triggerMode;
    private BigDecimal intervalKm;
    private Integer intervalDays;
    private BigDecimal dueSoonKm;
    private Integer dueSoonDays;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
