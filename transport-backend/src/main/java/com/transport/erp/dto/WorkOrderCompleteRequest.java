package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WorkOrderCompleteRequest {
    private String completionNotes;
    private BigDecimal actualCost;
}
