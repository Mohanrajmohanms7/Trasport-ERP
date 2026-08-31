package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CustomerReceiptPrintHistoryDTO {
    private Long receiptId;
    private long totalPrints;
    private long totalPdfExports;
    private long totalReprints;
    private LocalDateTime lastPrintedAt;
    private List<CustomerReceiptPrintAuditDTO> events;
}
