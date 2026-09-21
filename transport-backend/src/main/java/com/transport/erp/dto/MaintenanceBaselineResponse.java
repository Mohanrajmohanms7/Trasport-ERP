package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MaintenanceBaselineResponse {
    private Long id;
    private Long ruleId;
    private Long vehicleId;
    private BigDecimal lastServiceKm;
    private LocalDate lastServiceDate;
}
