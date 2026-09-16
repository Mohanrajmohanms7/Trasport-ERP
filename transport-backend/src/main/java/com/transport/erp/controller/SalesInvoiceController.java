package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.AppUser;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.SalesInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.transport.erp.dto.SalesInvoicePrintDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/invoices", "/api/v1/sales-invoices"})
@CrossOrigin(origins = "*")
public class SalesInvoiceController {

    @Autowired
    private SalesInvoiceService invoiceService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<SalesInvoice>> getInvoices(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<SalesInvoice> data = invoiceService.getInvoices(targetCompanyId, status, pageable);
        return ApiResponse.success(data, "Invoices fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesInvoice> getInvoiceById(@PathVariable Long id) {
        SalesInvoice invoice = invoiceService.getInvoiceById(id);
        return ApiResponse.success(invoice, "Invoice details fetched successfully");
    }

    @GetMapping("/{id}/print")
    public ApiResponse<SalesInvoicePrintDTO> getInvoicePrintData(@PathVariable Long id) {
        SalesInvoicePrintDTO data = invoiceService.getInvoicePrintData(id);
        return ApiResponse.success(data, "Sales invoice print data fetched successfully");
    }

    @GetMapping("/{id}/pdf")
    public void exportPdf(@PathVariable Long id, HttpServletResponse response) {
        try {
            SalesInvoicePrintDTO data = invoiceService.getInvoicePrintData(id);
            
            response.setContentType("application/pdf");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + data.getInvoiceNumber() + ".pdf");
            
            invoiceService.generateInvoicePdf(id, response.getOutputStream());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ApiResponse<SalesInvoice> create(@RequestBody SalesInvoice invoice) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        SalesInvoice created = invoiceService.createInvoice(invoice, activeUser);
        return ApiResponse.success(created, "Invoice created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<SalesInvoice> update(@PathVariable Long id, @RequestBody SalesInvoice invoice) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        SalesInvoice updated = invoiceService.updateInvoice(id, invoice, activeUser);
        return ApiResponse.success(updated, "Invoice updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        invoiceService.deleteInvoice(id, activeUser);
        return ApiResponse.success(null, "Invoice deleted successfully");
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<SalesInvoice> approve(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        SalesInvoice approved = invoiceService.approveInvoice(id, activeUser);
        return ApiResponse.success(approved, "Invoice approved successfully");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<SalesInvoice> cancel(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        SalesInvoice cancelled = invoiceService.cancelInvoice(id, activeUser);
        return ApiResponse.success(cancelled, "Invoice cancelled successfully");
    }

    @PostMapping("/from-trip/{tripId}")
    public ApiResponse<SalesInvoice> createFromTrip(@PathVariable Long tripId) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        SalesInvoice created = invoiceService.createInvoiceFromTrip(tripId, activeUser);
        return ApiResponse.success(created, "Draft invoice generated from completed trip successfully");
    }

    @GetMapping("/customer/{customerId}/outstanding")
    public ApiResponse<List<SalesInvoice>> getOutstandingInvoices(@PathVariable Long customerId) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = currentUser.getBranchId();
        List<SalesInvoice> outstanding = invoiceService.getOutstandingInvoices(customerId, companyId, branchId);
        return ApiResponse.success(outstanding, "Outstanding invoices fetched successfully");
    }

    @Autowired
    private com.transport.erp.service.XlsxExportService xlsxExportService;

    @GetMapping({"/export/xlsx", "/xlsx"})
    public void exportXlsx(@RequestParam(required = false) Long companyId, jakarta.servlet.http.HttpServletResponse response) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            byte[] bytes = xlsxExportService.exportSalesInvoices(scopedCompanyId);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_invoices_export.xlsx");
            response.getOutputStream().write(bytes);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

