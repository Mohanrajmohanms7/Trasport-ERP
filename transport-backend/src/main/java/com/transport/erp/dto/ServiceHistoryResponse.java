package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceHistoryResponse {
    private Long id;
    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private String sourceType;
    private Long sourceId;
    private LocalDate serviceDate;
    private String serviceType;
    private String description;
    private String workOrderNumber;
    private String supplierName;
    private BigDecimal odometerKm;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private BigDecimal operationalCost;
    private BigDecimal financialAmount;
    private String status;
    private String attachmentPath;
    private LocalDateTime createdAt;
}
