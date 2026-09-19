package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "vehicle_odometer_readings")
public class VehicleOdometerReading extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    @Column(name = "reading_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal readingKm;

    @Column(name = "reading_at", nullable = false)
    private LocalDateTime readingAt;

    @Column(nullable = false, length = 50)
    private String source; // OPENING, MANUAL, CORRECTION

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "fuel_entry_id")
    private Long fuelEntryId;

    @Column(name = "work_order_id")
    private Long workOrderId;
}
