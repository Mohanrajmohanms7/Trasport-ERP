package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "booking_details")
public class BookingDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonBackReference
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "transport_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal transportRate = BigDecimal.ZERO;

    @Column(name = "royalty_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal royaltyRate = BigDecimal.ZERO;

    @Column(name = "loading_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal loadingCharge = BigDecimal.ZERO;

    @Column(name = "gst_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.valueOf(18.00);

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;
}
