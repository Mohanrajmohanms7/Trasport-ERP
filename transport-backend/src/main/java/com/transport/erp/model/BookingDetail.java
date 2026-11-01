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

    /** PER_UNIT = transport rate x quantity (default); PER_TRIP = transport rate is a fixed amount per load, editable per trip (V90). */
    @Column(name = "transport_basis", nullable = false, length = 10)
    private String transportBasis = "PER_UNIT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonBackReference
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** Unit the quantity is in (UNIT, TON, KG …). Rates on this line are per this unit. No conversion is applied. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uom_id")
    private UomMaster uom;

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

    /** Unknown / empty basis = PER_UNIT (the original behaviour). */
    @PrePersist
    @PreUpdate
    void normaliseTransportBasis() {
        transportBasis = "PER_TRIP".equalsIgnoreCase(transportBasis == null ? "" : transportBasis.trim()) ? "PER_TRIP" : "PER_UNIT";
    }
}
