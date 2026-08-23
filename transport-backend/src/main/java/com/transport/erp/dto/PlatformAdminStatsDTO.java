package com.transport.erp.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PlatformAdminStatsDTO {
    private long totalCompanies;
    private long activeCompanies;
    private long totalUsers;
    private long totalVehicles;
    private long totalTrips;
    private long totalAuditLogs;
    private long activeLicenses;
    private long openSupportTickets;
    private double monthToDateRevenue;
}
