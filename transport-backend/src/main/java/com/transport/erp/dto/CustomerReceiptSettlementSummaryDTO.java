package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CustomerReceiptSettlementSummaryDTO {
    private long totalApprovedReceipts;
    private BigDecimal totalReceived = BigDecimal.ZERO;
    private BigDecimal totalAllocated = BigDecimal.ZERO;
    private BigDecimal totalAdvance = BigDecimal.ZERO;
    private BigDecimal totalCancelled = BigDecimal.ZERO;
    private long totalDraft;
    private BigDecimal totalOutstanding = BigDecimal.ZERO;
    private long totalCustomers;
    private long totalOutstandingInvoices;
    private long totalPartiallyPaidInvoices;
    private long totalPaidInvoices;
    private long totalUnpaidInvoices;
}
