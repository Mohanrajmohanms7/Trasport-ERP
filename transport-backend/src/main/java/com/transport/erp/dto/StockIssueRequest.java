package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockIssueRequest {
    private Long warehouseId;
    private Long workOrderId;
    private Long workOrderPartId;
    private BigDecimal quantity;
}
