package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MaintenanceRuleRequest {
    /** SUPER_ADMIN target company; ignored for tenant users via TenantAccessService.resolveCompanyId. */
    private Long companyId;
    private String name;
    private String description;
    private String maintenanceType;
    private String triggerMode;
    private BigDecimal intervalKm;
    private Integer intervalDays;
    private BigDecimal dueSoonKm;
    private Integer dueSoonDays;
    private String status;
}
