package com.transport.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GeneralLedgerReportDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private Long jvId;
        private LocalDate voucherDate;
        private String voucherNumber;
        private String referenceNumber;
        private String description;
        private BigDecimal debit;
        private BigDecimal credit;
        private BigDecimal runningBalance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private String accountType;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal openingBalance;
        private BigDecimal closingBalance;
        private BigDecimal totalDebit;
        private BigDecimal totalCredit;
        private List<Entry> entries;
    }
}
