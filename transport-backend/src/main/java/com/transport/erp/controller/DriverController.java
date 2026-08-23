package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Driver;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/drivers")
@CrossOrigin(origins = "*")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Driver>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Driver> drivers = driverService.getAll(scopedCompanyId, search, pageable);
            return ApiResponse.success(drivers, "Drivers fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch drivers");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Driver> getById(@PathVariable Long id) {
        try {
            return driverService.getById(id)
                    .map(d -> ApiResponse.success(d, "Driver fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Driver not found"), "Driver not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch driver");
        }
    }

    @PostMapping
    public ApiResponse<Driver> create(@RequestBody Driver driver) {
        try {
            driver.setCompanyId(tenantAccess.resolveCompanyId(driver.getCompanyId()));
            Driver created = driverService.create(driver);
            return ApiResponse.success(created, "Driver created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create driver");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Driver> update(@PathVariable Long id, @RequestBody Driver driver) {
        try {
            Driver updated = driverService.update(id, driver);
            return ApiResponse.success(updated, "Driver updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update driver");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            driverService.delete(id);
            return ApiResponse.success(null, "Driver deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete driver");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Driver> toggleStatus(@PathVariable Long id) {
        try {
            Driver toggled = driverService.toggleStatus(id);
            return ApiResponse.success(toggled, "Driver status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
