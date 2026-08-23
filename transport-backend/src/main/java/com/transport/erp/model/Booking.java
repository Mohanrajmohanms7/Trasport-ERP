package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Column(name = "booking_number", nullable = false, length = 100)
    private String bookingNumber;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_site_id")
    private CustomerDeliverySite deliverySite;

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // DRAFT, PENDING, APPROVED, REJECTED, ON_HOLD

    @Column(nullable = false, length = 50)
    private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<BookingDetail> details = new ArrayList<>();
}
