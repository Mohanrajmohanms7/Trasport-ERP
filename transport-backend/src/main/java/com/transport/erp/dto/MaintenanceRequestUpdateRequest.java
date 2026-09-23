package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceRequestUpdateRequest {
    private Long vehicleId;
    private String title;
    private String description;
    private String priority;
}
