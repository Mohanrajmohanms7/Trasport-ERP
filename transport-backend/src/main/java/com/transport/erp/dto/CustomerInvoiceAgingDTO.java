package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerInvoiceAgingDTO {
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private Long customerId;
    private String customerName;
    private BigDecimal invoiceAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal outstandingAmount = BigDecimal.ZERO;
    private String paymentStatus;
    private long agingDays;
    private String agingBucket;
}
