package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WorkOrderPartRequest {
    private Long sparePartId;
    private BigDecimal quantity;
    private BigDecimal unitRate;
    private String notes;
}
