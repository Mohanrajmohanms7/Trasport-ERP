package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryTransactionResponse {
    private Long id;
    private String code;
    private LocalDateTime createdDate;
    private String transactionType;
    private Long companyId;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long sparePartId;
    private String sparePartCode;
    private String sparePartName;
    private BigDecimal quantity;
    private String referenceType;
    private Long referenceId;
    private Long workOrderId;
    private String workOrderNumber;
    private Long workOrderPartId;
    private String createdBy;
    private String description;
}
