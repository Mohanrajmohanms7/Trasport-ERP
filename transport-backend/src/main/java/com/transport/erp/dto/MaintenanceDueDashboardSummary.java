package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceDueDashboardSummary {
    private long overdueCount;
    private long dueCount;
    private long dueSoonCount;
    private long unknownCount;
}
