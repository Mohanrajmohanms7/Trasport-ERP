package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.ChartOfAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@CrossOrigin(origins = "*")
public class ChartOfAccountController {

    @Autowired
    private ChartOfAccountService accountService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<ChartOfAccount>> getAccounts(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "accountCode"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<ChartOfAccount> data = accountService.getAccounts(targetCompanyId, pageable);
        return ApiResponse.success(data, "Chart of accounts fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<ChartOfAccount> getAccountById(@PathVariable Long id) {
        ChartOfAccount account = accountService.getAccountById(id);
        return ApiResponse.success(account, "Account details fetched successfully");
    }

    @PostMapping
    public ApiResponse<ChartOfAccount> create(@RequestBody ChartOfAccount account) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        ChartOfAccount created = accountService.createAccount(account, activeUser);
        return ApiResponse.success(created, "Account created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<ChartOfAccount> update(@PathVariable Long id, @RequestBody ChartOfAccount account) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        ChartOfAccount updated = accountService.updateAccount(id, account, activeUser);
        return ApiResponse.success(updated, "Account updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        accountService.deleteAccount(id, activeUser);
        return ApiResponse.success(null, "Account deleted successfully");
    }
}
