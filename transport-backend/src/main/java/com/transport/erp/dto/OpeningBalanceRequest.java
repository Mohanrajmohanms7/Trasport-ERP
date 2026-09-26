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
    /** Optional cost per unit; values the stock and posts it to the inventory account. */
    private BigDecimal unitRate;
    private String description;
}
