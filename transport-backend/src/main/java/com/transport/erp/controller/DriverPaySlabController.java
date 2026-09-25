package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.DriverPaySlab;
import com.transport.erp.service.DriverPaySlabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/driver-pay-slabs")
public class DriverPaySlabController {

    @Autowired
    private DriverPaySlabService slabService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<List<DriverPaySlab>> getSlabs(@RequestParam(required = false) Long companyId) {
        return ApiResponse.success(slabService.getSlabs(companyId), "Daily pay slabs");
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'ACCOUNTANT')")
    public ApiResponse<List<DriverPaySlab>> saveSlabs(@RequestParam(required = false) Long companyId,
                                                      @RequestBody List<DriverPaySlab> slabs, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        return ApiResponse.success(slabService.replaceSlabs(companyId, slabs, username), "Daily pay slabs saved");
    }
}
