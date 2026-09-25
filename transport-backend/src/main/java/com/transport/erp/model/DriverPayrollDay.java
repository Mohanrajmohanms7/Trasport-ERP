package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One calendar day of a payroll: completed trips that day and the slab amount earned. */
@Getter
@Setter
@Entity
@Table(name = "driver_payroll_days")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverPayrollDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_id", nullable = false)
    @JsonBackReference("payroll-days")
    private DriverPayroll payroll;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "trip_count", nullable = false)
    private Integer tripCount;

    @Column(name = "slab_trips_from")
    private Integer slabTripsFrom;

    /** null = no upper limit. */
    @Column(name = "slab_trips_to")
    private Integer slabTripsTo;

    @Column(name = "daily_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyAmount = BigDecimal.ZERO;
}
