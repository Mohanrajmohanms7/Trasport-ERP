package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "warehouse_stock")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WarehouseStock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id", nullable = false)
    private SparePart sparePart;

    @Column(name = "available_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal availableQuantity = BigDecimal.ZERO;

    /** Moving average cost per unit (receipts / opening balances with a rate). 0 = not valued. */
    @Column(name = "average_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal averageCost = BigDecimal.ZERO;
}
