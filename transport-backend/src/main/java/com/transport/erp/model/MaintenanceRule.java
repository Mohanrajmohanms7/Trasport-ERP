package com.transport.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "maintenance_rules")
public class MaintenanceRule extends BaseEntity {

    @Column(name = "maintenance_type", nullable = false, length = 50)
    private String maintenanceType;

    @Column(name = "trigger_mode", nullable = false, length = 20)
    private String triggerMode;

    @Column(name = "interval_km", precision = 12, scale = 2)
    private BigDecimal intervalKm;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "due_soon_km", precision = 12, scale = 2)
    private BigDecimal dueSoonKm;

    @Column(name = "due_soon_days")
    private Integer dueSoonDays;
}
