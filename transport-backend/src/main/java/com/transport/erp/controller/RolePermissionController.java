package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Role permissions screen: module x View / Add / Edit / Delete / Approve per role of the company (V92). */
@RestController
@RequestMapping("/api/v1/role-permissions")
public class RolePermissionController {

    @Autowired private RolePermissionService service;
    @Autowired private TenantAccessService tenantAccess;

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN')")
    public ApiResponse<List<String>> roles(@RequestParam(required = false) Long companyId) {
        return ApiResponse.success(service.manageableRoles(tenantAccess.resolveCompanyId(companyId)), "Roles");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN')")
    public ApiResponse<Map<String, Object>> grid(@RequestParam String role, @RequestParam(required = false) Long companyId) {
        return ApiResponse.success(service.roleGrid(tenantAccess.resolveCompanyId(companyId), role.toUpperCase()), "Role permissions");
    }

    @PutMapping("/{role}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN')")
    public ApiResponse<Map<String, Object>> save(@PathVariable String role, @RequestBody List<RolePermissionService.Row> rows,
                                                 @RequestParam(required = false) Long companyId, Authentication auth) {
        return ApiResponse.success(service.save(tenantAccess.resolveCompanyId(companyId), role, rows, auth.getName()), "Permissions saved");
    }

    @DeleteMapping("/{role}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'ADMIN')")
    public ApiResponse<Map<String, Object>> reset(@PathVariable String role, @RequestParam(required = false) Long companyId, Authentication auth) {
        return ApiResponse.success(service.reset(tenantAccess.resolveCompanyId(companyId), role, auth.getName()), "Permissions reset to full access");
    }

    /** The signed-in user's effective rights (screens hide menus / buttons with it). */
    @GetMapping("/me")
    public ApiResponse<Map<String, Map<String, Boolean>>> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a != null && a.startsWith("ROLE_")).map(a -> a.substring(5)).toList();
        Long cid = tenantAccess.resolveCompanyId(null);
        return ApiResponse.success(service.effective(cid == null ? 0L : cid, roles), "Your permissions");
    }
}
