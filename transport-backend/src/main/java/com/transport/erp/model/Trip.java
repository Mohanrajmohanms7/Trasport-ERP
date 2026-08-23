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

    @Column(nullable = false, length = 50)
    private String status = "PLANNED"; // PLANNED, DISPATCHED, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<TripDetail> details = new ArrayList<>();
}
