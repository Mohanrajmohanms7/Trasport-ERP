package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Quarry;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.QuarryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/quarries")
@CrossOrigin(origins = "*")
public class QuarryController {

    @Autowired
    private QuarryService quarryService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Quarry>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Quarry> quarries = quarryService.getAll(scopedCompanyId, search, pageable);
            return ApiResponse.success(quarries, "Quarries fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch quarries");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Quarry> getById(@PathVariable Long id) {
        try {
            return quarryService.getById(id)
                    .map(q -> ApiResponse.success(q, "Quarry fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Quarry not found"), "Quarry not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch quarry");
        }
    }

    @PostMapping
    public ApiResponse<Quarry> create(@RequestBody Quarry quarry) {
        try {
            quarry.setCompanyId(tenantAccess.resolveCompanyId(quarry.getCompanyId()));
            Quarry created = quarryService.create(quarry);
            return ApiResponse.success(created, "Quarry created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create quarry");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Quarry> update(@PathVariable Long id, @RequestBody Quarry quarry) {
        try {
            Quarry updated = quarryService.update(id, quarry);
            return ApiResponse.success(updated, "Quarry updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update quarry");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            quarryService.delete(id);
            return ApiResponse.success(null, "Quarry deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete quarry");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Quarry> toggleStatus(@PathVariable Long id) {
        try {
            Quarry toggled = quarryService.toggleStatus(id);
            return ApiResponse.success(toggled, "Quarry status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
