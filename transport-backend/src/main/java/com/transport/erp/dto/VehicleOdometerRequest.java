package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VehicleOdometerRequest {
    private BigDecimal readingKm;
    private String source;
    private LocalDateTime readingAt;
    private String reason;
}
