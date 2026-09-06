package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.FuelRequest;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.FuelRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fuel/request")
@CrossOrigin(origins = "*")
public class FuelRequestController {

    @Autowired
    private FuelRequestService requestService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<Page<FuelRequest>> getRequests(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<FuelRequest> data = requestService.getRequests(targetCompanyId, status, pageable);
        return ApiResponse.success(data, "Fuel Requests fetched successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<FuelRequest> getById(@PathVariable Long id) {
        FuelRequest req = requestService.getRequestById(id);
        return ApiResponse.success(req, "Fuel Request details fetched successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<FuelRequest> create(@RequestBody FuelRequest request) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        FuelRequest created = requestService.createRequest(request, activeUser);
        return ApiResponse.success(created, "Fuel Request registered successfully");
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<FuelRequest> approve(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        FuelRequest approved = requestService.approveRequest(id, activeUser);
        return ApiResponse.success(approved, "Fuel Request approved successfully");
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<FuelRequest> reject(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        FuelRequest rejected = requestService.rejectRequest(id, activeUser);
        return ApiResponse.success(rejected, "Fuel Request rejected successfully");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<FuelRequest> cancel(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        FuelRequest cancelled = requestService.cancelRequest(id, activeUser);
        return ApiResponse.success(cancelled, "Fuel Request cancelled successfully");
    }
}
