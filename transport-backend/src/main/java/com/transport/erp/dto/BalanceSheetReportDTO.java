package com.transport.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BalanceSheetReportDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private String accountType;
        private BigDecimal balance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private LocalDate asOfDate;
        private List<Row> assetRows;
        private List<Row> liabilityRows;
        private List<Row> equityRows;
        private BigDecimal totalAssets;
        private BigDecimal totalLiabilities;
        private BigDecimal totalEquity;
        private BigDecimal netProfitCurrentPeriod;
        private BigDecimal totalLiabilitiesAndEquity;
        private BigDecimal difference;
    }
}
