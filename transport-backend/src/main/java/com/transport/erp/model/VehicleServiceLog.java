package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "vehicle_services")
public class VehicleServiceLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType; // OIL_CHANGE, ENGINE_SERVICE, TYRE_CHANGE, BRAKE_SERVICE

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Column(length = 150)
    private String workshop;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
