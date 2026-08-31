package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerReceiptPrintAuditDTO {
    private String eventType;
    private String printedBy;
    private LocalDateTime printedAt;
    private String format;
}
