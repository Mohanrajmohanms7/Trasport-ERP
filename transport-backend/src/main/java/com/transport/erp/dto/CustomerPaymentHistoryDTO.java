package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerPaymentHistoryDTO {
    private Long receiptId;
    private String receiptNumber;
    private LocalDate receiptDate;
    private BigDecimal amountReceived;
    private BigDecimal allocatedAmount;
    private BigDecimal advanceAmount;
    private String paymentMethod;
    private String status;
    private String referenceNumber;
    private String remarks;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private Long customerId;
    private String customerName;
}
