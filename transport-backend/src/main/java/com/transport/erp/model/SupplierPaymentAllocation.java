package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Part of a supplier payment settled against one bill. */
@Getter
@Setter
@Entity
@Table(name = "supplier_payment_allocations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SupplierPaymentAllocation extends BaseEntity {
    @Column(name = "payment_id", nullable = false) private Long paymentId;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "bill_id", nullable = false) private SupplierBill bill;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount = BigDecimal.ZERO;
}
