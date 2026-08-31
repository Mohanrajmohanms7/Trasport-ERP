package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CustomerPaymentHistoryResponseDTO {
    // Customer Details
    private Long customerId;
    private String customerName;

    // Financial Summary
    private BigDecimal totalReceived;
    private BigDecimal totalAllocated;
    private BigDecimal totalAdvance;
    private BigDecimal totalCancelled;
    private long totalApprovedReceipts;
    private long totalCancelledReceipts;
    private long totalDraftReceipts;
    private BigDecimal totalOutstandingInvoices;

    // History Grid
    private List<CustomerPaymentHistoryDTO> history;
}
