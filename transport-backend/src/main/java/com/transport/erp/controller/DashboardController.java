package com.transport.erp.controller;

import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<Map<String, Object>> getAdminDashboard(@RequestParam(required = false) Long branchId) {
        return ApiResponse.success(dashboardService.getAdminDashboard(branchId),
                "Admin dashboard metrics fetched successfully");
    }

    @GetMapping("/owner")
    public ApiResponse<Map<String, Object>> getOwnerDashboard(@RequestParam(required = false) Long branchId) {
        return ApiResponse.success(dashboardService.getOwnerDashboard(branchId),
                "Owner dashboard metrics fetched successfully");
    }

    @GetMapping("/operations")
    public ApiResponse<Map<String, Object>> getOperationsDashboard(@RequestParam(required = false) Long branchId) {
        return ApiResponse.success(dashboardService.getOperationsDashboard(branchId),
                "Operations dashboard metrics fetched successfully");
    }

    @GetMapping("/vehicle")
    public ApiResponse<Map<String, Object>> getVehicleDashboard(@RequestParam(required = false) Long branchId) {
        return ApiResponse.success(dashboardService.getVehicleDashboard(branchId),
                "Vehicle Manager dashboard metrics fetched successfully");
    }

    @GetMapping("/account")
    public ApiResponse<Map<String, Object>> getAccountDashboard(@RequestParam(required = false) Long branchId) {
        return ApiResponse.success(dashboardService.getAccountDashboard(branchId),
                "Accountant dashboard metrics fetched successfully");
    }

    @GetMapping("/driver")
    public ApiResponse<Map<String, Object>> getDriverDashboard(@RequestParam(required = false) Long branchId) {
        return ApiResponse.success(dashboardService.getDriverDashboard(branchId),
                "Driver dashboard metrics fetched successfully");
    }
}
