package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CustomerLedgerPrintDTO {

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

    // Financial Summary
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;

    // Ledger Items
    private List<CustomerLedgerItemDTO> items;

    @Getter
    @Setter
    public static class CustomerLedgerItemDTO {
        private Long ledgerId;
        private LocalDateTime transactionDate;
        private String transactionType;
        private String referenceNumber;
        private String remarks;
        private BigDecimal debitAmount = BigDecimal.ZERO;
        private BigDecimal creditAmount = BigDecimal.ZERO;
        private BigDecimal runningBalance = BigDecimal.ZERO;
    }
}
