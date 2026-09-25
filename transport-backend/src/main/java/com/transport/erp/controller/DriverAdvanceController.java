package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.DriverAdvance;
import com.transport.erp.service.DriverAdvanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/driver-advances")
public class DriverAdvanceController {

    @Autowired
    private DriverAdvanceService advanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<Page<DriverAdvance>> list(@RequestParam(required = false) Long companyId,
                                                 @RequestParam(required = false) Long driverId,
                                                 @RequestParam(required = false) String status,
                                                 Pageable pageable) {
        return ApiResponse.success(advanceService.search(companyId, driverId, status, pageable), "Driver advances");
    }

    @GetMapping("/driver/{driverId}/outstanding")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<BigDecimal> outstanding(@PathVariable Long driverId) {
        return ApiResponse.success(advanceService.outstanding(driverId), "Outstanding advance");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<DriverAdvance> issue(@RequestBody DriverAdvance advance, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        return ApiResponse.success(advanceService.issue(advance, username), "Advance issued and posted");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'ACCOUNTANT')")
    public ApiResponse<DriverAdvance> cancel(@PathVariable Long id, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        return ApiResponse.success(advanceService.cancel(id, username), "Advance cancelled and reversed");
    }
}
