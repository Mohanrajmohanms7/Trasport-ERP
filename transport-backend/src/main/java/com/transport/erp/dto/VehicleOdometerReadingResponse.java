package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VehicleOdometerReadingResponse {
    private Long id;
    private BigDecimal readingKm;
    private LocalDateTime readingAt;
    private String source;
    private String reason;
    private String createdBy;
    private LocalDateTime createdDate;
}
