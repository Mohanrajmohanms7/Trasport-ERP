package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerReceiptListDTO {
    private Long id;
    private String receiptNumber;
    private String referenceNumber;
    private Long customerId;
    private String customerName;
    private LocalDate receiptDate;
    private BigDecimal amountReceived;
    private BigDecimal totalAllocated;
    private BigDecimal advanceAmount;
    private String paymentMethod;
    private String status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private Long branchId;
    private String branchName;
    private Long companyId;
    private LocalDateTime createdDate;
    private String createdBy;
}
