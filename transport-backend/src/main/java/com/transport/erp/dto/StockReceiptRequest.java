package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockReceiptRequest {
    private Long warehouseId;
    private Long sparePartId;
    private BigDecimal quantity;
    private BigDecimal unitRate;
    private Long supplierId;
    /** CREDIT (default), CASH or BANK. */
    private String paymentMode;
    private String referenceNumber;
    private String description;
}
