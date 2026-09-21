package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MaintenanceBaselineRequest {
    private BigDecimal lastServiceKm;
    private LocalDate lastServiceDate;
}
