package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.MaintenanceBaselineRequest;
import com.transport.erp.dto.MaintenanceBaselineResponse;
import com.transport.erp.dto.MaintenanceDueResponse;
import com.transport.erp.dto.MaintenanceRuleRequest;
import com.transport.erp.dto.MaintenanceRuleResponse;
import com.transport.erp.service.MaintenanceRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/maintenance")
@CrossOrigin(origins = "*")
public class MaintenanceRuleController {

    @Autowired
    private MaintenanceRuleService maintenanceRuleService;

    @GetMapping("/rules")
    public ApiResponse<Page<MaintenanceRuleResponse>> listRules(
            @RequestParam(required = false) Long companyId,
            Pageable pageable) {
        Page<MaintenanceRuleResponse> data = maintenanceRuleService.listRules(companyId, pageable);
        return ApiResponse.success(data, "Maintenance rules fetched successfully");
    }

    @GetMapping("/rules/{id}")
    public ApiResponse<MaintenanceRuleResponse> getRule(@PathVariable Long id) {
        MaintenanceRuleResponse data = maintenanceRuleService.getRule(id);
        return ApiResponse.success(data, "Maintenance rule fetched successfully");
    }

    @PostMapping("/rules")
    public ApiResponse<MaintenanceRuleResponse> createRule(
            @RequestBody MaintenanceRuleRequest request,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        MaintenanceRuleResponse data = maintenanceRuleService.createRule(request, username);
        return ApiResponse.success(data, "Maintenance rule created successfully");
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<MaintenanceRuleResponse> updateRule(
            @PathVariable Long id,
            @RequestBody MaintenanceRuleRequest request,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        MaintenanceRuleResponse data = maintenanceRuleService.updateRule(id, request, username);
        return ApiResponse.success(data, "Maintenance rule updated successfully");
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        maintenanceRuleService.deleteRule(id, username);
        return ApiResponse.success(null, "Maintenance rule deactivated successfully");
    }

    @PutMapping("/rules/{ruleId}/vehicles/{vehicleId}/baseline")
    public ApiResponse<MaintenanceBaselineResponse> upsertBaseline(
            @PathVariable Long ruleId,
            @PathVariable Long vehicleId,
            @RequestBody MaintenanceBaselineRequest request,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        MaintenanceBaselineResponse data = maintenanceRuleService.upsertBaseline(ruleId, vehicleId, request, username);
        return ApiResponse.success(data, "Maintenance baseline saved successfully");
    }

    @GetMapping("/due")
    public ApiResponse<Page<MaintenanceDueResponse>> getDue(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String maintenanceType,
            @RequestParam(required = false) String dueStatus,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long companyId,
            Pageable pageable) {
        Page<MaintenanceDueResponse> data = maintenanceRuleService.getDue(
                vehicleId, maintenanceType, dueStatus, branchId, companyId, pageable, null);
        return ApiResponse.success(data, "Maintenance due list fetched successfully");
    }
}
