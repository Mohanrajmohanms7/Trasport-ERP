package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "trip_details")
public class TripDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonBackReference
    private Trip trip;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "loading_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal loadingCharges = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal royalty = BigDecimal.ZERO;

    @Column(name = "dispatch_time")
    private LocalDateTime dispatchTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;
}
