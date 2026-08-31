package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.AppRole;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<List<AppRole>> getRoles(
            @RequestParam(required = false) Long companyId) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<AppRole> roles = roleService.getRoles(scopedCompanyId);
        return ApiResponse.success(roles, "Roles fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<AppRole> getRoleById(@PathVariable Long id) {
        AppRole role = roleService.getRoleById(id);
        return ApiResponse.success(role, "Role details fetched successfully");
    }

    @PostMapping
    public ApiResponse<AppRole> createRole(@Valid @RequestBody AppRole role) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        role.setCompanyId(tenantAccess.resolveCompanyId(role.getCompanyId()));
        role.setBranchId(tenantAccess.resolveBranchId(role.getBranchId()));


        AppRole created = roleService.createRole(role, activeUser);
        return ApiResponse.success(created, "Role created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<AppRole> updateRole(@PathVariable Long id, @Valid @RequestBody AppRole role) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        AppRole updated = roleService.updateRole(id, role, activeUser);
        return ApiResponse.success(updated, "Role details updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        roleService.deleteRole(id, activeUser);
        return ApiResponse.success(null, "Role soft deleted successfully");
    }
}
