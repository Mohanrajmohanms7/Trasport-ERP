package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class StockReceiptResponse {
    private Long transactionId;
    private String transactionCode;
    private String transactionType;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long sparePartId;
    private String sparePartCode;
    private String sparePartName;
    private BigDecimal quantity;
    private BigDecimal unitRate;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String externalReference;
    private BigDecimal availableQuantity;
    private LocalDateTime createdDate;
    private String createdBy;
}
