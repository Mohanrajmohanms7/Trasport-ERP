package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerPaymentPerformanceDTO {
    private Long customerId;
    private String customerName;
    private long totalReceipts;
    private BigDecimal totalReceived = BigDecimal.ZERO;
    private BigDecimal totalAllocated = BigDecimal.ZERO;
    private BigDecimal totalAdvance = BigDecimal.ZERO;
    private BigDecimal totalCancelled = BigDecimal.ZERO;
    private BigDecimal averageReceiptAmount = BigDecimal.ZERO;
    private LocalDate lastPaymentDate;
    private BigDecimal outstandingAmount = BigDecimal.ZERO;
    private long paymentCount;
}
