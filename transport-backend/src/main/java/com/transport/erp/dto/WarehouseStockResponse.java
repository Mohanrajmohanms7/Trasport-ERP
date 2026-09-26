package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WarehouseStockResponse {
    private Long id;
    private Long companyId;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long sparePartId;
    private String sparePartCode;
    private String sparePartName;
    private String uomCode;
    private String uomName;
    private BigDecimal availableQuantity;
    private BigDecimal averageCost;
    private BigDecimal stockValue;
    private BigDecimal reorderLevel;
    /** OK, REORDER (at or below reorder level) or OUT (zero). */
    private String stockStatus;
    private Integer version;
}
