package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CustomerReceiptDetailDTO {
    // Receipt Information
    private Long id;
    private String receiptNumber;
    private String referenceNumber;
    private LocalDate receiptDate;
    private String paymentMethod;
    private BigDecimal amountReceived;
    private BigDecimal advanceAmount;
    private String remarks;
    private String status;

    // Customer Information
    private Long customerId;
    private String customerName;

    // Audit Info
    private String createdBy;
    private LocalDateTime createdDate;
    private String approvedBy;
    private LocalDateTime approvedAt;

    // Tenant info
    private Long companyId;
    private Long branchId;

    // Allocation Summary
    private BigDecimal totalAllocated;
    private int allocationCount;
    private BigDecimal totalInvoiceOutstandingAfterAllocation;

    // Allocation details
    private List<CustomerReceiptAllocationDTO> allocations;

    // Accounting References
    private String customerLedgerReference;
    private String journalVoucherReference;
    private String reversalJournalVoucherReference;

    // Audit Timeline
    private List<CustomerReceiptAuditDTO> auditTimeline;
}
