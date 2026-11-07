package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "trips")
public class Trip extends BaseEntity {

    /** Driver bata the CUSTOMER pays directly to the driver (V91). Shown on the invoice PDF for information only; never in
     *  invoice amounts, GST, receivables, accounts or payroll. */
    @Column(name = "customer_paid_bata", precision = 12, scale = 2)
    private java.math.BigDecimal customerPaidBata;

    /**
     * Request-only: the user confirmed using a driver other than the vehicle's regular (Vehicle Master) driver for this
     * trip. Not stored; the trip's driver is the actual driver. Vehicle Master is never changed by a trip.
     */
    @Transient
    private Boolean confirmDriverOverride;

    @Column(name = "trip_number", nullable = false, length = 100)
    private String tripNumber;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quarry_id")
    private Quarry quarry;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "loading_location_id")
    private LoadingLocation loadingLocation;

    @Column(nullable = false, length = 50)
    private String status = "PLANNED"; // PLANNED, DISPATCHED, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<TripDetail> details = new ArrayList<>();

    @Transient
    private String billingStatus;

    @Transient
    private String associatedInvoiceNumber;

    @Transient
    private Long associatedInvoiceId;
}

