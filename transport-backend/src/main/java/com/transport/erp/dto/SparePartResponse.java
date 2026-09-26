package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SparePartResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private Long companyId;
    private Long defaultUomId;
    private String defaultUomCode;
    private String defaultUomName;
    private BigDecimal defaultRate;
    private String photoFile;
    private BigDecimal reorderLevel;
    private Integer version;
}
