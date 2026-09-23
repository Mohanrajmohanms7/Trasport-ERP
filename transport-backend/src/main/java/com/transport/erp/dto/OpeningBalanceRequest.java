package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OpeningBalanceRequest {
    private Long warehouseId;
    private Long sparePartId;
    private BigDecimal quantity;
    private String description;
}
