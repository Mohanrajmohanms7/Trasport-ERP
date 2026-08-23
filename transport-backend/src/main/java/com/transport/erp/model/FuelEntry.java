package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "fuel_entries")
public class FuelEntry extends BaseEntity {

    @Column(name = "fuel_entry_number", nullable = false, length = 100)
    private String fuelEntryNumber;

    @Column(name = "fuel_date", nullable = false)
    private LocalDate fuelDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "fuel_station", nullable = false, length = 150)
    private String fuelStation;

    @Column(name = "fuel_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal fuelQuantity = BigDecimal.ZERO;

    @Column(name = "rate_per_litre", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerLitre = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod = "CASH"; // CASH, UPI, BANK, CREDIT

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "current_odometer", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentOdometer = BigDecimal.ZERO;

    @Column(name = "previous_odometer", nullable = false, precision = 10, scale = 2)
    private BigDecimal previousOdometer = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
