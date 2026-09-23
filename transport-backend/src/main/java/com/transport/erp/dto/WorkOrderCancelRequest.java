package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkOrderCancelRequest {
    private String cancellationReason;
}
