package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarehouseRequest {
    private String code;
    private String name;
    private String description;
    private String status;
    private Long branchId;
    private Long companyId;
}
