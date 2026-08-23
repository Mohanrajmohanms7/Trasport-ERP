package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "customer_ledgers")
public class CustomerLedger extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receipt_id")
    private CustomerReceipt receipt;

    @Column(name = "debit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "running_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal runningBalance = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
