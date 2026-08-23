package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Material;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/materials")
@CrossOrigin(origins = "*")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Material>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Material> materials = materialService.getAll(scopedCompanyId, search, pageable);
            return ApiResponse.success(materials, "Materials fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch materials");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Material> getById(@PathVariable Long id) {
        try {
            return materialService.getById(id)
                    .map(m -> ApiResponse.success(m, "Material fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Material not found"), "Material not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch material");
        }
    }

    @PostMapping
    public ApiResponse<Material> create(@RequestBody Material material) {
        try {
            material.setCompanyId(tenantAccess.resolveCompanyId(material.getCompanyId()));
            Material created = materialService.create(material);
            return ApiResponse.success(created, "Material created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create material");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Material> update(@PathVariable Long id, @RequestBody Material material) {
        try {
            Material updated = materialService.update(id, material);
            return ApiResponse.success(updated, "Material updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update material");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            materialService.delete(id);
            return ApiResponse.success(null, "Material deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete material");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Material> toggleStatus(@PathVariable Long id) {
        try {
            Material toggled = materialService.toggleStatus(id);
            return ApiResponse.success(toggled, "Material status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
