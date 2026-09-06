package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.VehicleServiceLog;
import com.transport.erp.service.VehicleServiceLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}")
@CrossOrigin(origins = "*")
public class VehicleServiceLogController {

    @Autowired
    private VehicleServiceLogService logService;

    @GetMapping("/maintenance")
    public ApiResponse<List<VehicleServiceLog>> getMaintenanceHistory(@PathVariable Long vehicleId) {
        List<VehicleServiceLog> logs = logService.getLogsByVehicle(vehicleId);
        return ApiResponse.success(logs, "Maintenance logs fetched successfully");
    }

    @GetMapping("/service/{id}")
    public ApiResponse<VehicleServiceLog> getServiceLog(
            @PathVariable Long vehicleId,
            @PathVariable Long id) {
        VehicleServiceLog log = logService.getLogById(vehicleId, id);
        return ApiResponse.success(log, "Service log fetched successfully");
    }

    @PostMapping("/service")
    public ApiResponse<VehicleServiceLog> addServiceLog(
            @PathVariable Long vehicleId,
            @RequestBody VehicleServiceLog log,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        VehicleServiceLog created = logService.addLog(vehicleId, log, username);
        return ApiResponse.success(created, "Maintenance service logged successfully");
    }

    @PutMapping("/service/{id}")
    public ApiResponse<VehicleServiceLog> updateServiceLog(
            @PathVariable Long vehicleId,
            @PathVariable Long id,
            @RequestBody VehicleServiceLog details,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        VehicleServiceLog updated = logService.updateLog(vehicleId, id, details, username);
        return ApiResponse.success(updated, "Maintenance service log updated successfully");
    }

    @PostMapping("/service/{id}/approve")
    public ApiResponse<VehicleServiceLog> approveServiceLog(
            @PathVariable Long vehicleId,
            @PathVariable Long id,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        VehicleServiceLog approved = logService.approveLog(vehicleId, id, username);
        return ApiResponse.success(approved, "Maintenance service log approved and posted to accounting successfully");
    }

    @PostMapping("/service/{id}/cancel")
    public ApiResponse<VehicleServiceLog> cancelServiceLog(
            @PathVariable Long vehicleId,
            @PathVariable Long id,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        VehicleServiceLog cancelled = logService.cancelLog(vehicleId, id, username);
        return ApiResponse.success(cancelled, "Maintenance service log cancelled and reversal posted successfully");
    }

    @DeleteMapping("/service/{id}")
    public ApiResponse<Void> deleteServiceLog(
            @PathVariable Long vehicleId,
            @PathVariable Long id,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        logService.deleteLog(vehicleId, id, username);
        return ApiResponse.success(null, "Maintenance service log deleted successfully");
    }
}
