package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VehicleOdometerResponse {
    private Long vehicleId;
    private BigDecimal currentOdometerKm;
    private LocalDateTime odometerUpdatedAt;
    private VehicleOdometerReadingResponse latestReading;
    private List<VehicleOdometerReadingResponse> recentReadings = new ArrayList<>();
}
