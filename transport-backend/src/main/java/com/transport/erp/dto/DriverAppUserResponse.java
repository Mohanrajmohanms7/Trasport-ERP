package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverAppUserResponse {
    private Long driverId;
    private Long companyId;
    private Long appUserId;
    private String appUserName;
    private String mappingStatus;
}
