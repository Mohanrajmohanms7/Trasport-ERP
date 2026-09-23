package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WorkOrderPartResponse {
    private Long id;
    private String code;
    private Long sparePartId;
    private String sparePartCode;
    private String sparePartName;
    private BigDecimal quantity;
    private BigDecimal unitRate;
    private BigDecimal lineTotal;
    private Long uomId;
    private String uomCode;
    private String uomName;
    private String notes;
    private Integer version;
}
