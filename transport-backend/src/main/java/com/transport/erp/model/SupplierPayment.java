package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Money paid to a supplier, allocated to bills. Status POSTED or CANCELLED. */
@Getter
@Setter
@Entity
@Table(name = "supplier_payments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SupplierPayment extends BaseEntity {
    @Column(name = "payment_number", nullable = false, length = 50) private String paymentNumber;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "supplier_id", nullable = false) private Supplier supplier;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "payment_method", nullable = false, length = 20) private String paymentMethod;
    @Column(name = "reference_number", length = 100) private String referenceNumber;
    @Column(name = "jv_reference", length = 100) private String jvReference;
    @Column(columnDefinition = "TEXT") private String remarks;
}
