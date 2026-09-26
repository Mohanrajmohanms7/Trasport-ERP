package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class MaintenanceRequestResponse {
    private Long id;
    private String requestNumber;
    private Long vehicleId;
    private String vehicleCode;
    private String vehicleName;
    private String vehicleRegistrationNumber;
    private Long companyId;
    private Long branchId;
    private Long requestedByUserId;
    private String requestedByUsername;
    private Long driverId;
    private String driverName;
    private String status;
    private String priority;
    private String title;
    private String description;
    private LocalDateTime requestedAt;
    private BigDecimal reportedOdometerKm;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewRemarks;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private Long workOrderId;
    private String workOrderNumber;
    /** Current status of the work order this request became (OPEN, IN_PROGRESS, COMPLETED, CANCELLED). */
    private String workOrderStatus;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
    private Integer version;
}
