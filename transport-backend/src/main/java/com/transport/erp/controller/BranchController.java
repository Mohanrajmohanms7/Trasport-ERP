package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Branch;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.BranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/branches")
@CrossOrigin(origins = "*")
public class BranchController {

    @Autowired
    private BranchService branchService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Branch>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Branch> branches = branchService.getAll(scopedCompanyId, search, pageable);
            return ApiResponse.success(branches, "Branches fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch branches");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Branch> getById(@PathVariable Long id) {
        try {
            return branchService.getById(id)
                    .map(b -> {
                        tenantAccess.assertCompanyAccess(b.getCompanyId());
                        return ApiResponse.success(b, "Branch fetched successfully");
                    })
                    .orElse(ApiResponse.error(Collections.singletonList("Branch not found"), "Branch not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch branch");
        }
    }

    @PostMapping
    public ApiResponse<Branch> create(@RequestBody Branch branch) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(branch.getCompanyId());
            branch.setCompanyId(scopedCompanyId);
            Branch created = branchService.create(branch);
            return ApiResponse.success(created, "Branch created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create branch");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Branch> update(@PathVariable Long id, @RequestBody Branch branch) {
        try {
            Branch existing = branchService.getById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
            tenantAccess.assertCompanyAccess(existing.getCompanyId());
            branch.setCompanyId(existing.getCompanyId());
            Branch updated = branchService.update(id, branch);
            return ApiResponse.success(updated, "Branch updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update branch");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            Branch existing = branchService.getById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
            tenantAccess.assertCompanyAccess(existing.getCompanyId());
            branchService.delete(id);
            return ApiResponse.success(null, "Branch deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete branch");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Branch> toggleStatus(@PathVariable Long id) {
        try {
            Branch existing = branchService.getById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
            tenantAccess.assertCompanyAccess(existing.getCompanyId());
            Branch toggled = branchService.toggleStatus(id);
            return ApiResponse.success(toggled, "Branch status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
