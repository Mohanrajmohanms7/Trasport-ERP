package com.transport.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ProfitLossReportDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private String accountType;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<Row> incomeRows;
        private List<Row> expenseRows;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netProfit;
        private BigDecimal netLoss;
        private Boolean isProfitable;
    }
}
