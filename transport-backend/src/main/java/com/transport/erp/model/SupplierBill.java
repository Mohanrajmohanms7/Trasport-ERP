package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A supplier's bill we owe. Manual (approved -> posted) or created by a credit stock receipt / workshop job. */
@Getter
@Setter
@Entity
@Table(name = "supplier_bills")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SupplierBill extends BaseEntity {
    @Column(name = "bill_number", nullable = false, length = 50) private String billNumber;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "supplier_id", nullable = false) private Supplier supplier;
    @Column(name = "supplier_bill_no", length = 60) private String supplierBillNo;
    @Column(name = "bill_date", nullable = false) private LocalDate billDate;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(nullable = false, length = 20) private String category;
    @Column(name = "taxable_amount", nullable = false, precision = 14, scale = 2) private BigDecimal taxableAmount = BigDecimal.ZERO;
    @Column(name = "gst_amount", nullable = false, precision = 14, scale = 2) private BigDecimal gstAmount = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2) private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2) private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name = "payment_status", nullable = false, length = 20) private String paymentStatus = "UNPAID";
    @Column(name = "source_type", nullable = false, length = 30) private String sourceType = "MANUAL";
    @Column(name = "source_id") private Long sourceId;
    @Column(name = "source_reference", length = 100) private String sourceReference;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "vehicle_id") private Vehicle vehicle;
    @Column(name = "jv_reference", length = 100) private String jvReference;
    @Column(columnDefinition = "TEXT") private String remarks;

    @Transient
    public BigDecimal getBalance() {
        return "APPROVED".equals(getStatus()) ? totalAmount.subtract(paidAmount == null ? BigDecimal.ZERO : paidAmount) : BigDecimal.ZERO;
    }
}
