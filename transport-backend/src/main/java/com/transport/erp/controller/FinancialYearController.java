package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.FinancialYear;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.FinancialYearService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/financial-years")
@CrossOrigin(origins = "*")
public class FinancialYearController {

    @Autowired
    private FinancialYearService fyService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<FinancialYear>> getFinancialYears(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<FinancialYear> data = fyService.getFinancialYears(targetCompanyId, pageable);
        return ApiResponse.success(data, "Financial Years fetched successfully");
    }

    @PostMapping
    public ApiResponse<FinancialYear> create(@RequestBody FinancialYear fy) {
        fy.setCompanyId(tenantAccess.resolveCompanyId(fy.getCompanyId()));
        FinancialYear created = fyService.create(fy);
        return ApiResponse.success(created, "Financial Year registered successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<FinancialYear> update(@PathVariable Long id, @RequestBody FinancialYear fy) {
        FinancialYear existing = fyService.getById(id);
        tenantAccess.assertCompanyAccess(existing.getCompanyId());
        fy.setCompanyId(existing.getCompanyId());
        FinancialYear updated = fyService.update(id, fy);
        return ApiResponse.success(updated, "Financial Year details updated successfully");
    }
}
