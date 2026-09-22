package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MaintenanceDueDashboardResponse {
    private MaintenanceDueDashboardSummary summary = new MaintenanceDueDashboardSummary();
    private List<MaintenanceDueDashboardItem> alerts = new ArrayList<>();

    public static MaintenanceDueDashboardResponse empty() {
        return new MaintenanceDueDashboardResponse();
    }
}
