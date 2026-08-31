package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerReceiptAllocationDTO {
    private Long allocationId;
    private Long receiptId;
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal invoiceTotal;
    private BigDecimal allocatedAmount;
    private BigDecimal invoicePaidAmount;
    private BigDecimal invoiceOutstandingAmount;
    private String paymentStatus;
}
