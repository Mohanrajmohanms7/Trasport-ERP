package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Company;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@CrossOrigin(origins = "*")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Company>> getAll(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            AppUser user = tenantAccess.requireCurrentUser();
            if (!tenantAccess.isSuperAdmin(user)) {
                if (user.getCompanyId() == null) {
                    throw new AccessDeniedException("User is not assigned to a company");
                }
                Company own = companyService.getById(user.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
                Page<Company> single = new PageImpl<>(List.of(own), pageable, 1);
                return ApiResponse.success(single, "Companies fetched successfully");
            }
            Page<Company> companies = companyService.getAll(search, pageable);
            return ApiResponse.success(companies, "Companies fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch companies");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Company> getById(@PathVariable Long id) {
        try {
            tenantAccess.assertCompanyAccess(id);
            return companyService.getById(id)
                    .map(c -> ApiResponse.success(c, "Company fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Company not found"), "Company not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch company");
        }
    }

    @PostMapping
    public ApiResponse<Company> create(@RequestBody Company company) {
        try {
            if (!tenantAccess.isSuperAdmin()) {
                throw new AccessDeniedException("Only platform admin can create companies");
            }
            Company created = companyService.create(company);
            return ApiResponse.success(created, "Company created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create company");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Company> update(@PathVariable Long id, @RequestBody Company company) {
        try {
            tenantAccess.assertCompanyAccess(id);
            Company updated = companyService.update(id, company);
            return ApiResponse.success(updated, "Company updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update company");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            if (!tenantAccess.isSuperAdmin()) {
                throw new AccessDeniedException("Only platform admin can delete companies");
            }
            companyService.delete(id);
            return ApiResponse.success(null, "Company deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete company");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Company> toggleStatus(@PathVariable Long id) {
        try {
            if (!tenantAccess.isSuperAdmin()) {
                throw new AccessDeniedException("Only platform admin can change company status");
            }
            Company toggled = companyService.toggleStatus(id);
            return ApiResponse.success(toggled, "Company status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
