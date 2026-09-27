package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.SupplierBill;
import com.transport.erp.model.SupplierPayment;
import com.transport.erp.model.SupplierPaymentAllocation;
import com.transport.erp.service.PayablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payables")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'VIEWER')")
public class PayablesController {

    @Autowired
    private PayablesService payables;

    private static String user(Principal p) {
        return p != null ? p.getName() : "system";
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'VIEWER')")
    @GetMapping("/bills")
    public ApiResponse<Page<SupplierBill>> bills(@RequestParam(required = false) Long companyId, @RequestParam(required = false) Long supplierId,
                                                 @RequestParam(required = false) String status, @RequestParam(required = false) String paymentStatus,
                                                 @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                                 @RequestParam(defaultValue = "300") int size) {
        return ApiResponse.success(payables.bills(companyId, supplierId, status, paymentStatus, from, to, size), "Supplier bills");
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'VIEWER')")
    @GetMapping("/suppliers/{supplierId}/open-bills")
    public ApiResponse<List<SupplierBill>> openBills(@PathVariable Long supplierId) {
        return ApiResponse.success(payables.openBills(supplierId), "Open bills");
    }

    @PostMapping("/bills")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<SupplierBill> createBill(@RequestBody SupplierBill bill, Principal p) {
        return ApiResponse.success(payables.createBill(bill, user(p)), "Bill saved as draft");
    }

    @PutMapping("/bills/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<SupplierBill> updateBill(@PathVariable Long id, @RequestBody SupplierBill bill, Principal p) {
        return ApiResponse.success(payables.updateBill(id, bill, user(p)), "Bill updated");
    }

    @PostMapping("/bills/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'ACCOUNTANT')")
    public ApiResponse<SupplierBill> approveBill(@PathVariable Long id, Principal p) {
        return ApiResponse.success(payables.approveBill(id, user(p)), "Bill approved and posted");
    }

    @PostMapping("/bills/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'ACCOUNTANT')")
    public ApiResponse<SupplierBill> cancelBill(@PathVariable Long id, Principal p) {
        return ApiResponse.success(payables.cancelBill(id, user(p)), "Bill cancelled");
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'VIEWER')")
    @GetMapping("/payments")
    public ApiResponse<Page<SupplierPayment>> payments(@RequestParam(required = false) Long companyId, @RequestParam(required = false) Long supplierId,
                                                       @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                                       @RequestParam(defaultValue = "300") int size) {
        return ApiResponse.success(payables.payments(companyId, supplierId, from, to, size), "Supplier payments");
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT', 'VIEWER')")
    @GetMapping("/payments/{id}/allocations")
    public ApiResponse<List<SupplierPaymentAllocation>> allocations(@PathVariable Long id) {
        return ApiResponse.success(payables.paymentAllocations(id), "Allocations");
    }

    public record PaymentRequest(Long supplierId, LocalDate paymentDate, BigDecimal amount, String paymentMethod, String referenceNumber,
                                 String remarks, List<PayablesService.Allocation> allocations) { }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'ACCOUNTANT')")
    public ApiResponse<SupplierPayment> pay(@RequestBody PaymentRequest r, Principal p) {
        return ApiResponse.success(payables.pay(r.supplierId(), r.paymentDate(), r.amount(), r.paymentMethod(), r.referenceNumber(),
                r.remarks(), r.allocations(), user(p)), "Payment posted");
    }

    @PostMapping("/payments/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPANY_ADMIN', 'ACCOUNTANT')")
    public ApiResponse<SupplierPayment> cancelPayment(@PathVariable Long id, Principal p) {
        return ApiResponse.success(payables.cancelPayment(id, user(p)), "Payment cancelled and reversed");
    }
}
