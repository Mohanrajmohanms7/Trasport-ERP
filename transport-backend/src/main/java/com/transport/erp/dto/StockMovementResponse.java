package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockMovementResponse {
    private Long transactionId;
    private String transactionCode;
    private String transactionType;
    private Long workOrderId;
    private String workOrderNumber;
    private Long workOrderPartId;
    private Long sparePartId;
    private String sparePartCode;
    private String sparePartName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private BigDecimal quantity;
    private BigDecimal issuedQuantity;
    private BigDecimal returnedQuantity;
    private BigDecimal netIssuedQuantity;
    private BigDecimal remainingToIssue;
    private BigDecimal returnableQuantity;
    private BigDecimal availableQuantity;
}
