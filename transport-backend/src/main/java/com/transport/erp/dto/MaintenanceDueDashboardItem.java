package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MaintenanceDueDashboardItem {
    private Long vehicleId;
    private String vehicleCode;
    private String vehicleName;
    private String maintenanceType;
    private String triggerMode;
    private BigDecimal currentOdometerKm;
    private BigDecimal lastServiceKm;
    private LocalDate lastServiceDate;
    private BigDecimal nextDueKm;
    private LocalDate nextDueDate;
    private BigDecimal remainingKm;
    private Long remainingDays;
    private String dueStatus;
}
