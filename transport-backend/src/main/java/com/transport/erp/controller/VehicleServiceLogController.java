package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.VehicleServiceLog;
import com.transport.erp.service.VehicleServiceLogService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/service")
    public ApiResponse<VehicleServiceLog> addServiceLog(
            @PathVariable Long vehicleId,
            @RequestBody VehicleServiceLog log) {
        VehicleServiceLog created = logService.addLog(vehicleId, log);
        return ApiResponse.success(created, "Maintenance service logged successfully");
    }
}
