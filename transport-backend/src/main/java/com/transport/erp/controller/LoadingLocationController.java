package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.LoadingLocation;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.LoadingLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loading-locations")
@CrossOrigin(origins = "*")
public class LoadingLocationController {

    @Autowired
    private LoadingLocationService locationService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<List<LoadingLocation>> getLocations(
            @RequestParam(required = false) Long companyId) {
        List<LoadingLocation> locations = locationService.getLoadingLocations(companyId);
        return ApiResponse.success(locations, "Loading Locations fetched successfully");
    }

    @PostMapping
    public ApiResponse<LoadingLocation> create(@RequestBody LoadingLocation location) {
        location.setCompanyId(tenantAccess.resolveCompanyId(location.getCompanyId()));
        LoadingLocation created = locationService.create(location);
        return ApiResponse.success(created, "Loading Location registered successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<LoadingLocation> update(@PathVariable Long id, @RequestBody LoadingLocation location) {
        LoadingLocation updated = locationService.update(id, location);
        return ApiResponse.success(updated, "Loading Location details updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ApiResponse.success(null, "Loading Location deleted successfully");
    }
}
