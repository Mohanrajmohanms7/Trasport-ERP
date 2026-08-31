package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.CustomerReceiptDTO;
import com.transport.erp.dto.CustomerReceiptResponseDTO;
import com.transport.erp.dto.CustomerReceiptListDTO;
import com.transport.erp.dto.CustomerReceiptDetailDTO;
import com.transport.erp.dto.CustomerReceiptAllocationDTO;
import com.transport.erp.dto.CustomerPaymentHistoryResponseDTO;
import com.transport.erp.dto.CustomerReceiptAuditDTO;
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
import java.time.LocalDate;
import java.util.List;
import com.transport.erp.dto.CustomerReceiptPrintDTO;
import com.transport.erp.dto.CustomerReceiptPrintHistoryDTO;
import com.transport.erp.dto.CustomerReceiptSettlementSummaryDTO;
import com.transport.erp.dto.CustomerOutstandingSummaryDTO;
import com.transport.erp.dto.CustomerInvoiceAgingDTO;
import com.transport.erp.dto.CustomerAgingSummaryDTO;
import com.transport.erp.dto.ReceiptSettlementReconciliationDTO;
import com.transport.erp.dto.CustomerPaymentPerformanceDTO;
import com.transport.erp.dto.ReceiptSettlementDashboardDTO;
import com.transport.erp.util.ReceiptPdfGenerator;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/v1/receipts")
@CrossOrigin(origins = "*")
public class CustomerReceiptController {

    @Autowired
    private CustomerReceiptService receiptService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<CustomerReceiptListDTO>> getReceipts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("receiptDate"), Sort.Order.desc("id")));
        Page<CustomerReceiptListDTO> data = receiptService.getReceipts(search, status, paymentMethod, customerId, fromDate, toDate, pageable);
        return ApiResponse.success(data, "Customer receipts fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerReceiptDetailDTO> getReceiptById(@PathVariable Long id) {
        CustomerReceiptDetailDTO details = receiptService.getReceiptDetails(id);
        return ApiResponse.success(details, "Customer receipt details fetched successfully");
    }

    @GetMapping("/{receiptId}/allocations")
    public ApiResponse<List<CustomerReceiptAllocationDTO>> getReceiptAllocations(@PathVariable Long receiptId) {
        List<CustomerReceiptAllocationDTO> data = receiptService.getReceiptAllocations(receiptId);
        return ApiResponse.success(data, "Receipt allocations fetched successfully");
    }

    @GetMapping("/{receiptId}/audit")
    public ApiResponse<List<CustomerReceiptAuditDTO>> getReceiptAuditTrail(@PathVariable Long receiptId) {
        List<CustomerReceiptAuditDTO> data = receiptService.getReceiptAuditTrail(receiptId);
        return ApiResponse.success(data, "Receipt audit timeline fetched successfully");
    }

    @GetMapping("/customer/{customerId}/history")
    public ApiResponse<CustomerPaymentHistoryResponseDTO> getCustomerPaymentHistory(@PathVariable Long customerId) {
        CustomerPaymentHistoryResponseDTO data = receiptService.getCustomerPaymentHistory(customerId);
        return ApiResponse.success(data, "Customer payment history fetched successfully");
    }

    @PostMapping
    public ApiResponse<CustomerReceiptResponseDTO> create(@RequestBody CustomerReceiptDTO receipt) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        CustomerReceiptResponseDTO created = receiptService.createReceiptWithAllocations(receipt, activeUser);
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

    @PostMapping("/{receiptId}/cancel")
    public ApiResponse<CustomerReceiptResponseDTO> cancel(@PathVariable Long receiptId) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        CustomerReceiptResponseDTO cancelled = receiptService.cancelReceipt(receiptId, activeUser);
        return ApiResponse.success(cancelled, "Customer receipt cancelled successfully");
    }

    @PostMapping("/{receiptId}/approve")
    public ApiResponse<CustomerReceiptResponseDTO> approve(@PathVariable Long receiptId) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        CustomerReceiptResponseDTO approved = receiptService.approveReceipt(receiptId, activeUser);
        return ApiResponse.success(approved, "Customer receipt approved successfully");
    }

    @GetMapping("/{receiptId}/print")
    public ApiResponse<CustomerReceiptPrintDTO> getReceiptPrintData(@PathVariable Long receiptId) {
        CustomerReceiptPrintDTO data = receiptService.getReceiptPrintData(receiptId);
        return ApiResponse.success(data, "Customer receipt print data fetched successfully");
    }

    @GetMapping("/{receiptId}/print-history")
    public ApiResponse<CustomerReceiptPrintHistoryDTO> getReceiptPrintHistory(@PathVariable Long receiptId) {
        CustomerReceiptPrintHistoryDTO data = receiptService.getReceiptPrintHistory(receiptId);
        return ApiResponse.success(data, "Customer receipt print history fetched successfully");
    }

    @PostMapping("/{receiptId}/print-audit")
    public ApiResponse<Void> recordPrintEvent(@PathVariable Long receiptId, @RequestBody java.util.Map<String, String> payload) {
        String eventType = payload.get("eventType");
        String format = payload.get("format");
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        receiptService.recordReceiptPrintEvent(receiptId, eventType, format, activeUser);
        return ApiResponse.success(null, "Customer receipt print audit registered successfully");
    }

    @GetMapping("/{receiptId}/pdf")
    public void exportPdf(@PathVariable Long receiptId, HttpServletResponse response) {
        try {
            CustomerReceiptPrintDTO data = receiptService.getReceiptPrintData(receiptId);
            
            response.setContentType("application/pdf");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt_" + data.getReceiptNumber() + ".pdf");
            
            ReceiptPdfGenerator.generateReceiptPdf(data, response.getOutputStream());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/dashboard/summary")
    public ApiResponse<CustomerReceiptSettlementSummaryDTO> getDashboardSummary(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(tenantAccess.requireCurrentUser()) ? null : tenantAccess.requireCurrentUser().getBranchId();
        CustomerReceiptSettlementSummaryDTO data = receiptService.getSettlementSummary(companyId, branchId, fromDate, toDate);
        return ApiResponse.success(data, "Dashboard summary stats fetched successfully");
    }

    @GetMapping("/dashboard/customers")
    public ApiResponse<Page<CustomerOutstandingSummaryDTO>> getDashboardCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(tenantAccess.requireCurrentUser()) ? null : tenantAccess.requireCurrentUser().getBranchId();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerOutstandingSummaryDTO> data = receiptService.getCustomerOutstandingSummary(companyId, branchId, search, pageable);
        return ApiResponse.success(data, "Customer outstanding summaries fetched successfully");
    }

    @GetMapping("/dashboard/aging")
    public ApiResponse<CustomerAgingSummaryDTO> getDashboardAging(
            @RequestParam(required = false) Long customerId) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(tenantAccess.requireCurrentUser()) ? null : tenantAccess.requireCurrentUser().getBranchId();
        CustomerAgingSummaryDTO data = receiptService.getAgingSummary(companyId, branchId, customerId);
        return ApiResponse.success(data, "Customer invoice aging summary fetched successfully");
    }

    @GetMapping("/dashboard/aging/invoices")
    public ApiResponse<Page<CustomerInvoiceAgingDTO>> getDashboardAgingInvoices(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(tenantAccess.requireCurrentUser()) ? null : tenantAccess.requireCurrentUser().getBranchId();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerInvoiceAgingDTO> data = receiptService.getCustomerInvoiceAging(companyId, branchId, customerId, bucket, search, pageable);
        return ApiResponse.success(data, "Invoice aging details page fetched successfully");
    }

    @GetMapping("/dashboard/reconciliation")
    public ApiResponse<Page<ReceiptSettlementReconciliationDTO>> getDashboardReconciliation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(tenantAccess.requireCurrentUser()) ? null : tenantAccess.requireCurrentUser().getBranchId();
        Pageable pageable = PageRequest.of(page, size);
        Page<ReceiptSettlementReconciliationDTO> data = receiptService.getReceiptSettlementReconciliation(companyId, branchId, pageable);
        return ApiResponse.success(data, "Payment allocations reconciliation page fetched successfully");
    }

    @GetMapping("/dashboard/payment-performance")
    public ApiResponse<Page<CustomerPaymentPerformanceDTO>> getDashboardPaymentPerformance(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(tenantAccess.requireCurrentUser()) ? null : tenantAccess.requireCurrentUser().getBranchId();
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerPaymentPerformanceDTO> data = receiptService.getCustomerPaymentPerformance(companyId, branchId, pageable);
        return ApiResponse.success(data, "Customer payment performance page fetched successfully");
    }

    @GetMapping("/dashboard")
    public ApiResponse<ReceiptSettlementDashboardDTO> getDashboard() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(tenantAccess.requireCurrentUser()) ? null : tenantAccess.requireCurrentUser().getBranchId();
        ReceiptSettlementDashboardDTO data = receiptService.getSettlementDashboard(companyId, branchId);
        return ApiResponse.success(data, "Dashboard root stats payload fetched successfully");
    }
}


