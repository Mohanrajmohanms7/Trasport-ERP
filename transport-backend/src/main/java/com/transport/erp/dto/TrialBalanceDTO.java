package com.transport.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TrialBalanceDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private String accountType;
        private BigDecimal openingBalance;
        private BigDecimal periodDebit;
        private BigDecimal periodCredit;
        private BigDecimal closingDebit;
        private BigDecimal closingCredit;
        private BigDecimal closingBalance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<Row> rows;
        private BigDecimal totalOpeningBalance;
        private BigDecimal totalPeriodDebit;
        private BigDecimal totalPeriodCredit;
        private BigDecimal totalClosingDebit;
        private BigDecimal totalClosingCredit;
        private BigDecimal difference;
    }
}
