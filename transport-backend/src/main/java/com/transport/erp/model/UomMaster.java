package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "uom_master")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UomMaster extends BaseEntity {

    @Column(length = 20)
    private String symbol;

    @Column(nullable = false, length = 50)
    private String category = "GENERAL";

    @Column(name = "is_base_unit", nullable = false)
    private Boolean isBaseUnit = false;
}
