package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class WorkOrderResponse {
    private Long id;
    private String code;
    private String workOrderNumber;
    private String name;
    private String description;
    private String status;
    private Long companyId;
    private Long branchId;
    private String source;
    private Long vehicleId;
    private String vehicleCode;
    private String vehicleName;
    private Long maintenanceRuleId;
    private String maintenanceRuleName;
    private String maintenanceType;
    private String triggerMode;
    private String dueStatusAtCreation;
    private BigDecimal dueKm;
    private LocalDate dueDate;
    private BigDecimal baselineLastServiceKm;
    private LocalDate baselineLastServiceDate;
    private String priority;
    private LocalDateTime openedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDate requestedDate;
    private BigDecimal odometerAtOpen;
    private BigDecimal odometerAtComplete;
    private Long supplierId;
    private String supplierName;
    private Long assignedUserId;
    private String assignedUserName;
    private String diagnosis;
    private String completionNotes;
    private String cancellationReason;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private String attachmentPath;
    private String createdBy;
    private String completedBy;
    private String cancelledBy;
    private Integer version;
    private List<WorkOrderPartResponse> parts;
    private List<WorkOrderLabourResponse> labour;
    private BigDecimal partsTotal;
    private BigDecimal labourTotal;
    private BigDecimal operationalCost;
    private BigDecimal financialAmount;
    private String accountingStatus;
    private Long journalVoucherId;
    private String journalVoucherReference;
    private LocalDateTime postedAt;
}
