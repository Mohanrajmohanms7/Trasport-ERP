package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CustomerReceiptDTO {
    private Long id;
    private Long customerId;
    private LocalDate receiptDate;
    private BigDecimal amountReceived;
    private BigDecimal advanceAmount;
    private String paymentMethod;
    private String referenceNumber;
    private String remarks;
    private List<AllocationDTO> allocations;

    @Getter
    @Setter
    public static class AllocationDTO {
        private Long invoiceId;
        private BigDecimal amount;
    }
}
