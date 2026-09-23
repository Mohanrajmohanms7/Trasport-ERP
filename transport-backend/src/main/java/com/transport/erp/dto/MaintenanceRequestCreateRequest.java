package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Caller-editable request fields. Company, branch, number, requester, driver,
 * status, and odometer are assigned by the server.
 */
@Getter
@Setter
public class MaintenanceRequestCreateRequest {
    private Long vehicleId;
    private String title;
    private String description;
    private String priority;
}
