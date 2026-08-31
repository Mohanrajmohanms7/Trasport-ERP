package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CustomerReceiptResponseDTO {
    private Long receiptId;
    private String receiptNumber;
    private Long customerId;
    private String customerName;
    private LocalDate receiptDate;
    private BigDecimal paymentAmount;
    private String paymentMethod;
    private List<AllocationResponseDTO> allocations;
    private BigDecimal totalAllocated;
    private BigDecimal advanceAmount;
    private String status;
    private String referenceNumber;
    private String remarks;
    private String approvedBy;
    private java.time.LocalDateTime approvedAt;

    @Getter
    @Setter
    public static class AllocationResponseDTO {
        private Long invoiceId;
        private String invoiceNumber;
        private BigDecimal allocatedAmount;
        private BigDecimal invoiceTotal;
        private BigDecimal paidAmount;
        private BigDecimal outstandingAmount;
        private String paymentStatus;
    }
}

