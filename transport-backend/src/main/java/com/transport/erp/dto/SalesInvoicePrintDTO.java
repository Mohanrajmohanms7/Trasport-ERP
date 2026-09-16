package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SalesInvoicePrintDTO {

    // Invoice Meta
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String status; // DRAFT, PENDING, APPROVED, GENERATED, CANCELLED
    private String paymentTerms;
    private String paymentStatus; // UNPAID, PARTIALLY_PAID, PAID

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

    // Financial Breakdown
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal taxableAmount = BigDecimal.ZERO;
    private BigDecimal totalCGST = BigDecimal.ZERO;
    private BigDecimal totalSGST = BigDecimal.ZERO;
    private BigDecimal totalIGST = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal balanceDue = BigDecimal.ZERO;

    // Line Items
    private List<SalesInvoicePrintLineItemDTO> items;

    @Getter
    @Setter
    public static class SalesInvoicePrintLineItemDTO {
        private Long detailId;
        private Long tripId;
        private String tripNumber;
        private Long materialId;
        private String materialName;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal rate = BigDecimal.ZERO;
        private BigDecimal freightCharges = BigDecimal.ZERO;
        private BigDecimal loadingCharges = BigDecimal.ZERO;
        private BigDecimal royalty = BigDecimal.ZERO;
        private BigDecimal gstPercentage = BigDecimal.ZERO;
        private BigDecimal cgst = BigDecimal.ZERO;
        private BigDecimal sgst = BigDecimal.ZERO;
        private BigDecimal igst = BigDecimal.ZERO;
        private BigDecimal lineSubtotal = BigDecimal.ZERO;
        private BigDecimal lineTax = BigDecimal.ZERO;
        private BigDecimal netAmount = BigDecimal.ZERO;
    }
}
