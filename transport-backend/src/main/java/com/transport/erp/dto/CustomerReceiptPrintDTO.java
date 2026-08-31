package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CustomerReceiptPrintDTO {

    // Receipt Information
    private Long receiptId;
    private String receiptNumber;
    private String referenceNumber;
    private LocalDate receiptDate;
    private String status;
    private String paymentMethod;
    private BigDecimal amountReceived;
    private String remarks;

    // Customer Information
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerAddress;
    private String customerPhone;
    private String customerEmail;
    private String customerGSTIN;

    // Company Information
    private Long companyId;
    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    private String companyGSTIN;
    private String companyPAN;

    // Branch Information
    private Long branchId;
    private String branchName;
    private String branchAddress;
    private String branchPhone;

    // Approval Information
    private String approvedBy;
    private LocalDateTime approvedAt;

    // Cancellation Information
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private Boolean isCancelled;

    // Allocation Summary
    private BigDecimal totalAllocated;
    private BigDecimal totalAdvance;
    private Integer allocationCount;

    // Allocation Details
    private List<AllocationDetailDTO> allocations;

    // Accounting Summary
    private String debitAccount;
    private String creditAccount;
    private String journalVoucherReference;

    @Getter
    @Setter
    public static class AllocationDetailDTO {
        private Long invoiceId;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private BigDecimal invoiceTotal;
        private BigDecimal allocatedAmount;
        private BigDecimal invoicePaidAmount; // before this payment
        private BigDecimal paidAfterReceipt; // invoicePaidAmount + allocatedAmount
        private BigDecimal invoiceOutstanding; // outstanding before this payment
        private BigDecimal outstandingAfterReceipt; // invoiceOutstanding - allocatedAmount
        private String paymentStatus;
    }
}
