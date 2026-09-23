package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarehouseResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private Long companyId;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private Integer version;
}
