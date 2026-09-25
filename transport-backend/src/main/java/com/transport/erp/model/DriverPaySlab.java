package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Company daily pay slab: completed trips in one day between tripsFrom and tripsTo earn dailyAmount. */
@Getter
@Setter
@Entity
@Table(name = "driver_pay_slabs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverPaySlab extends BaseEntity {

    @Column(name = "trips_from", nullable = false)
    private Integer tripsFrom;

    /** null = no upper limit ("2 or more"). */
    @Column(name = "trips_to")
    private Integer tripsTo;

    @Column(name = "daily_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyAmount = BigDecimal.ZERO;
}
