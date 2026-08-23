package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    @Column(name = "expense_number", nullable = false, length = 100)
    private String expenseNumber;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, length = 100)
    private String category; // TOLL, DRIVER_BATA, PARKING, VEHICLE_REPAIR, INSURANCE, OFFICE

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "gst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod = "CASH"; // CASH, UPI, BANK_TRANSFER, CHEQUE, CREDIT

    @Column(nullable = false, length = 50)
    private String status = "SUBMITTED"; // DRAFT, SUBMITTED, APPROVED, REJECTED, PAID, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
