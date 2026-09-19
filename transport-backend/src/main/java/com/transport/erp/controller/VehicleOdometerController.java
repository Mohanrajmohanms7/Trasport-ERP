package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.VehicleOdometerRequest;
import com.transport.erp.dto.VehicleOdometerResponse;
import com.transport.erp.service.VehicleOdometerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/odometer")
@CrossOrigin(origins = "*")
public class VehicleOdometerController {

    @Autowired
    private VehicleOdometerService odometerService;

    @GetMapping
    public ApiResponse<VehicleOdometerResponse> getOdometer(@PathVariable Long vehicleId) {
        VehicleOdometerResponse data = odometerService.getOdometer(vehicleId);
        return ApiResponse.success(data, "Vehicle odometer fetched successfully");
    }

    @PostMapping
    public ApiResponse<VehicleOdometerResponse> recordOdometer(
            @PathVariable Long vehicleId,
            @RequestBody VehicleOdometerRequest request,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        VehicleOdometerResponse data = odometerService.recordOdometer(vehicleId, request, username);
        return ApiResponse.success(data, "Vehicle odometer recorded successfully");
    }
}
