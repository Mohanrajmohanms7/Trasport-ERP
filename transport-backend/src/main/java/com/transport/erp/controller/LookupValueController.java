package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.LookupValue;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.LookupValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lookups")
@CrossOrigin(origins = "*")
public class LookupValueController {

    @Autowired
    private LookupValueService lookupValueService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<LookupValue>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam String type,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<LookupValue> lookups = lookupValueService.getAllByType(scopedCompanyId, type, search, pageable);
            return ApiResponse.success(lookups, "Lookup values fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch lookup values");
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<LookupValue>> getList(
            @RequestParam(required = false) Long companyId,
            @RequestParam String type) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            List<LookupValue> list = lookupValueService.getListByType(scopedCompanyId, type);
            return ApiResponse.success(list, "Lookup values list fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch lookup values list");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<LookupValue> getById(@PathVariable Long id) {
        try {
            return lookupValueService.getById(id)
                    .map(l -> ApiResponse.success(l, "Lookup value fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Lookup value not found"), "Lookup value not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch lookup value");
        }
    }

    @PostMapping
    public ApiResponse<LookupValue> create(@RequestBody LookupValue lookupValue) {
        try {
            lookupValue.setCompanyId(tenantAccess.resolveCompanyId(lookupValue.getCompanyId()));
            LookupValue created = lookupValueService.create(lookupValue);
            return ApiResponse.success(created, "Lookup value created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create lookup value");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<LookupValue> update(@PathVariable Long id, @RequestBody LookupValue lookupValue) {
        try {
            LookupValue existing = lookupValueService.getById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Lookup value not found"));
            tenantAccess.assertCompanyAccess(existing.getCompanyId());
            LookupValue updated = lookupValueService.update(id, lookupValue);
            return ApiResponse.success(updated, "Lookup value updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update lookup value");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            LookupValue existing = lookupValueService.getById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Lookup value not found"));
            tenantAccess.assertCompanyAccess(existing.getCompanyId());
            lookupValueService.delete(id);
            return ApiResponse.success(null, "Lookup value deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete lookup value");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<LookupValue> toggleStatus(@PathVariable Long id) {
        try {
            LookupValue existing = lookupValueService.getById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Lookup value not found"));
            tenantAccess.assertCompanyAccess(existing.getCompanyId());
            LookupValue toggled = lookupValueService.toggleStatus(id);
            return ApiResponse.success(toggled, "Lookup value status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
