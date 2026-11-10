package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.DriverAppUserRequest;
import com.transport.erp.dto.DriverAppUserResponse;
import com.transport.erp.model.Driver;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
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
            @RequestParam(required = false) String status,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Driver> drivers = driverService.getAll(scopedCompanyId, search, status, pageable);
            return ApiResponse.success(drivers, "Drivers fetched successfully");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // → 403 via GlobalExceptionHandler
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
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // → 403 via GlobalExceptionHandler
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
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // → 403 via GlobalExceptionHandler
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create driver");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Driver> update(@PathVariable Long id, @RequestBody Driver driver) {
        try {
            Driver updated = driverService.update(id, driver);
            return ApiResponse.success(updated, "Driver updated successfully");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // → 403 via GlobalExceptionHandler
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update driver");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            driverService.delete(id);
            return ApiResponse.success(null, "Driver deleted successfully");
        } catch (com.transport.erp.exception.BusinessValidationException e) {
            return ApiResponse.error(e.getErrors(), e.getTitle() != null ? e.getTitle() : e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // → 403 via GlobalExceptionHandler
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete driver");
        }
    }

    /** Driver login in one step from Driver Master: create (+ link), reset password, enable / disable. Admin roles only. */
    public record DriverLoginBody(String username, String password, Boolean active) {}

    @PostMapping("/{id}/login")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<java.util.Map<String, Object>> createLogin(@PathVariable Long id, @RequestBody DriverLoginBody body, Authentication auth) {
        Driver d = driverService.createLogin(id, body == null ? null : body.username(), body == null ? null : body.password(), auth != null ? auth.getName() : "SYSTEM");
        return ApiResponse.success(java.util.Map.of("driverId", d.getId(), "appUserId", d.getAppUserId()), "Driver login created and linked");
    }

    @PostMapping("/{id}/login/password")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<String> resetLoginPassword(@PathVariable Long id, @RequestBody DriverLoginBody body, Authentication auth) {
        driverService.resetLoginPassword(id, body == null ? null : body.password(), auth != null ? auth.getName() : "SYSTEM");
        return ApiResponse.success("OK", "Password changed; the driver is signed out everywhere");
    }

    @PostMapping("/{id}/login/active")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<String> setLoginActive(@PathVariable Long id, @RequestBody DriverLoginBody body, Authentication auth) {
        boolean active = body != null && Boolean.TRUE.equals(body.active());
        driverService.setLoginActive(id, active, auth != null ? auth.getName() : "SYSTEM");
        return ApiResponse.success("OK", active ? "Driver login enabled" : "Driver login disabled");
    }

    @PutMapping("/{id}/app-user")
    public ApiResponse<DriverAppUserResponse> linkAppUser(
            @PathVariable Long id,
            @RequestBody DriverAppUserRequest request,
            Authentication auth) {
        Driver linked = driverService.linkAppUser(
                id,
                request == null ? null : request.getAppUserId(),
                auth != null ? auth.getName() : "SYSTEM");
        DriverAppUserResponse body = new DriverAppUserResponse();
        body.setDriverId(linked.getId());
        body.setCompanyId(linked.getCompanyId());
        body.setAppUserId(linked.getAppUserId());
        body.setAppUserName(linked.getAppUserName());
        body.setMappingStatus(linked.getAppUserId() == null ? "NOT_LINKED" : "LINKED");
        return ApiResponse.success(body, "Driver login updated successfully");
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Driver> toggleStatus(@PathVariable Long id) {
        try {
            Driver toggled = driverService.toggleStatus(id);
            return ApiResponse.success(toggled, "Driver status toggled successfully");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e; // → 403 via GlobalExceptionHandler
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }

    @Autowired
    private com.transport.erp.service.XlsxExportService xlsxExportService;

    @GetMapping({"/export/xlsx", "/xlsx"})
    public void exportXlsx(@RequestParam(required = false) Long companyId, jakarta.servlet.http.HttpServletResponse response) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            byte[] bytes = xlsxExportService.exportDrivers(scopedCompanyId);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=drivers_export.xlsx");
            response.getOutputStream().write(bytes);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
