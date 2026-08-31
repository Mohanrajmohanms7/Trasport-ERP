package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ReceiptSettlementReconciliationDTO {
    private Long receiptId;
    private String receiptNumber;
    private LocalDate receiptDate;
    private Long customerId;
    private String customerName;
    private String status;
    private BigDecimal amountReceived = BigDecimal.ZERO;
    private BigDecimal totalAllocated = BigDecimal.ZERO;
    private BigDecimal advanceAmount = BigDecimal.ZERO;
    private BigDecimal reconciliationDifference = BigDecimal.ZERO;
    private long allocationCount;
    private String reconciliationStatus; // BALANCED, MISMATCH
}
