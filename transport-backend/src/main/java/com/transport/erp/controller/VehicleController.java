package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Vehicle;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Vehicle>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Vehicle> vehicles = vehicleService.getAll(scopedCompanyId, search, pageable);
            return ApiResponse.success(vehicles, "Vehicles fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch vehicles");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Vehicle> getById(@PathVariable Long id) {
        try {
            return vehicleService.getById(id)
                    .map(v -> ApiResponse.success(v, "Vehicle fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Vehicle not found"), "Vehicle not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch vehicle");
        }
    }

    @PostMapping
    public ApiResponse<Vehicle> create(@RequestBody Vehicle vehicle) {
        try {
            vehicle.setCompanyId(tenantAccess.resolveCompanyId(vehicle.getCompanyId()));
            Vehicle created = vehicleService.create(vehicle);
            return ApiResponse.success(created, "Vehicle created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create vehicle");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Vehicle> update(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        try {
            Vehicle updated = vehicleService.update(id, vehicle);
            return ApiResponse.success(updated, "Vehicle updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update vehicle");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            vehicleService.delete(id);
            return ApiResponse.success(null, "Vehicle deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete vehicle");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Vehicle> toggleStatus(@PathVariable Long id) {
        try {
            Vehicle toggled = vehicleService.toggleStatus(id);
            return ApiResponse.success(toggled, "Vehicle status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
