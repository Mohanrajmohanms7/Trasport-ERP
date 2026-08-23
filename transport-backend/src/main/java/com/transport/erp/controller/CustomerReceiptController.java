package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.CustomerReceipt;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.CustomerReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
@CrossOrigin(origins = "*")
public class CustomerReceiptController {

    @Autowired
    private CustomerReceiptService receiptService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<CustomerReceipt>> getReceipts(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<CustomerReceipt> data = receiptService.getReceipts(targetCompanyId, pageable);
        return ApiResponse.success(data, "Receipts fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerReceipt> getReceiptById(@PathVariable Long id) {
        CustomerReceipt receipt = receiptService.getReceiptById(id);
        return ApiResponse.success(receipt, "Receipt details fetched successfully");
    }

    @PostMapping
    public ApiResponse<CustomerReceipt> create(@RequestBody CustomerReceipt receipt) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        CustomerReceipt created = receiptService.createReceipt(receipt, activeUser);
        return ApiResponse.success(created, "Receipt voucher registered successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerReceipt> update(@PathVariable Long id, @RequestBody CustomerReceipt receipt) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        CustomerReceipt updated = receiptService.updateReceipt(id, receipt, activeUser);
        return ApiResponse.success(updated, "Receipt details updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        receiptService.deleteReceipt(id, activeUser);
        return ApiResponse.success(null, "Receipt deleted successfully");
    }
}
