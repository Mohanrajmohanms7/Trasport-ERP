package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "fuel_requests")
public class FuelRequest extends BaseEntity {

    @Column(name = "request_number", nullable = false, length = 100)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "requested_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQuantity = BigDecimal.ZERO;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;
}
