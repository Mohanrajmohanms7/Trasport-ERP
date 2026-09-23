package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SparePartRequest {
    private String code;
    private String name;
    private String description;
    private Long defaultUomId;
    private BigDecimal defaultRate;
    private String status;
    private Long companyId;
}
