package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cash advance given to a driver; recovered through posted payrolls. Status ISSUED or CANCELLED. */
@Getter
@Setter
@Entity
@Table(name = "driver_advances")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DriverAdvance extends BaseEntity {

    @Column(name = "advance_number", nullable = false, length = 50)
    private String advanceNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "recovered_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal recoveredAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "jv_number", length = 100)
    private String jvNumber;

    @Column(name = "cancellation_jv_number", length = 100)
    private String cancellationJvNumber;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Transient
    public BigDecimal getOutstandingAmount() {
        if (!"ISSUED".equals(getStatus())) return BigDecimal.ZERO;
        return amount.subtract(recoveredAmount == null ? BigDecimal.ZERO : recoveredAmount);
    }
}
