package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.JournalVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/journal")
@CrossOrigin(origins = "*")
public class JournalVoucherController {

    @Autowired
    private JournalVoucherService voucherService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<JournalVoucher>> getVouchers(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<JournalVoucher> data = voucherService.getVouchers(targetCompanyId, pageable);
        return ApiResponse.success(data, "Journal vouchers fetched successfully");
    }

    @PostMapping
    public ApiResponse<JournalVoucher> create(@RequestBody JournalVoucher voucher) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        JournalVoucher created = voucherService.createVoucher(voucher, activeUser);
        return ApiResponse.success(created, "Journal entry posted successfully");
    }
}
