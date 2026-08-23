package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/admin")
    public ApiResponse<Map<String, Object>> getAdminDashboard() {
        return ApiResponse.success(dashboardService.getAdminDashboard(),
                "Admin dashboard metrics fetched successfully");
    }

    @GetMapping("/owner")
    public ApiResponse<Map<String, Object>> getOwnerDashboard() {
        return ApiResponse.success(dashboardService.getOwnerDashboard(),
                "Owner dashboard metrics fetched successfully");
    }

    @GetMapping("/operations")
    public ApiResponse<Map<String, Object>> getOperationsDashboard() {
        return ApiResponse.success(dashboardService.getOperationsDashboard(),
                "Operations dashboard metrics fetched successfully");
    }

    @GetMapping("/vehicle")
    public ApiResponse<Map<String, Object>> getVehicleDashboard() {
        return ApiResponse.success(dashboardService.getVehicleDashboard(),
                "Vehicle Manager dashboard metrics fetched successfully");
    }

    @GetMapping("/account")
    public ApiResponse<Map<String, Object>> getAccountDashboard() {
        return ApiResponse.success(dashboardService.getAccountDashboard(),
                "Accountant dashboard metrics fetched successfully");
    }

    @GetMapping("/driver")
    public ApiResponse<Map<String, Object>> getDriverDashboard() {
        return ApiResponse.success(dashboardService.getDriverDashboard(),
                "Driver dashboard metrics fetched successfully");
    }
}
