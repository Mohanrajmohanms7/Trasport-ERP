package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerReceiptAuditDTO {
    private String eventType;
    private LocalDateTime eventTime;
    private String performedBy;
    private String remarks;
}
