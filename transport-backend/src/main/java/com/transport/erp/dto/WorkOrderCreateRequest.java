package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create payload. Company, branch, status, number, odometer, and due snapshots
 * on this object are ignored. Those values are taken from the authorized vehicle
 * and, for preventive jobs, from MaintenanceDueCalculator.
 */
@Getter
@Setter
public class WorkOrderCreateRequest {
    private Long vehicleId;
    private String source;
    private Long maintenanceRuleId;
    private String maintenanceType;
    private String name;
    private String description;
    private String diagnosis;
    private String priority;
    private Long supplierId;
    private Long assignedUserId;
    private BigDecimal estimatedCost;
    private LocalDate requestedDate;
    private String attachmentPath;

    private Long companyId;
    private Long branchId;
    private String status;
    private String workOrderNumber;
    private BigDecimal odometerAtOpen;
    private BigDecimal odometerAtComplete;
    private String dueStatusAtCreation;
    private BigDecimal dueKm;
    private LocalDate dueDate;
    private BigDecimal baselineLastServiceKm;
    private LocalDate baselineLastServiceDate;
}
