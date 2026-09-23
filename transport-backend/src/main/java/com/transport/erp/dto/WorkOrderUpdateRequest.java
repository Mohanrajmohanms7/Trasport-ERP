package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderUpdateRequest {
    private String name;
    private String description;
    private String priority;
    private Long supplierId;
    private Long assignedUserId;
    private BigDecimal estimatedCost;
    private LocalDate requestedDate;
    private String diagnosis;
    private String attachmentPath;

    private Long vehicleId;
    private String source;
    private Long maintenanceRuleId;
    private String maintenanceType;
    private String status;
    private Long companyId;
    private Long branchId;
    private BigDecimal odometerAtOpen;
    private BigDecimal odometerAtComplete;
    private BigDecimal actualCost;
    private String completionNotes;
    private String dueStatusAtCreation;
    private BigDecimal dueKm;
    private LocalDate dueDate;
    private BigDecimal baselineLastServiceKm;
    private LocalDate baselineLastServiceDate;
}
