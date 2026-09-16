package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Expense;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Expense>> getExpenses(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<Expense> data = expenseService.getExpenses(targetCompanyId, status, pageable);
        return ApiResponse.success(data, "Expenses fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<Expense> getExpenseById(@PathVariable Long id) {
        Expense expense = expenseService.getExpenseById(id);
        return ApiResponse.success(expense, "Expense details fetched successfully");
    }

    @PostMapping
    public ApiResponse<Expense> create(@RequestBody Expense expense) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Expense created = expenseService.createExpense(expense, activeUser);
        return ApiResponse.success(created, "Expense voucher registered successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<Expense> update(@PathVariable Long id, @RequestBody Expense expense) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Expense updated = expenseService.updateExpense(id, expense, activeUser);
        return ApiResponse.success(updated, "Expense details updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        expenseService.deleteExpense(id, activeUser);
        return ApiResponse.success(null, "Expense deleted successfully");
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Expense> approve(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Expense approved = expenseService.approveExpense(id, activeUser);
        return ApiResponse.success(approved, "Expense approved successfully");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Expense> reject(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Expense rejected = expenseService.rejectExpense(id, activeUser);
        return ApiResponse.success(rejected, "Expense rejected successfully");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Expense> cancel(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Expense cancelled = expenseService.cancelExpense(id, activeUser);
        return ApiResponse.success(cancelled, "Expense cancelled successfully");
    }

    @Autowired
    private com.transport.erp.service.XlsxExportService xlsxExportService;

    @GetMapping({"/export/xlsx", "/xlsx"})
    public void exportXlsx(@RequestParam(required = false) Long companyId, jakarta.servlet.http.HttpServletResponse response) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            byte[] bytes = xlsxExportService.exportExpenses(scopedCompanyId);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses_export.xlsx");
            response.getOutputStream().write(bytes);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
