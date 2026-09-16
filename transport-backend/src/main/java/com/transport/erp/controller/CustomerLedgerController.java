package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.CustomerLedgerPrintDTO;
import com.transport.erp.model.CustomerLedger;
import com.transport.erp.service.CustomerLedgerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer-ledger")
@CrossOrigin(origins = "*")
public class CustomerLedgerController {

    @Autowired
    private CustomerLedgerService ledgerService;

    @GetMapping("/{customerId}")
    public ApiResponse<List<CustomerLedger>> getLedger(@PathVariable Long customerId) {
        List<CustomerLedger> ledger = ledgerService.getLedgerByCustomer(customerId);
        return ApiResponse.success(ledger, "Customer ledger fetched successfully");
    }

    @GetMapping("/{customerId}/print")
    public ApiResponse<CustomerLedgerPrintDTO> getLedgerPrintData(@PathVariable Long customerId) {
        CustomerLedgerPrintDTO data = ledgerService.getLedgerPrintData(customerId);
        return ApiResponse.success(data, "Customer ledger print data fetched successfully");
    }

    @GetMapping("/{customerId}/pdf")
    public void exportPdf(@PathVariable Long customerId, HttpServletResponse response) {
        try {
            CustomerLedgerPrintDTO data = ledgerService.getLedgerPrintData(customerId);
            String safeCode = data.getCustomerCode() != null ? data.getCustomerCode() : String.valueOf(customerId);

            response.setContentType("application/pdf");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer_ledger_" + safeCode + ".pdf");

            ledgerService.generateLedgerPdf(customerId, response.getOutputStream());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping({"/{customerId}/csv", "/{customerId}/export"})
    public void exportCsv(@PathVariable Long customerId, HttpServletResponse response) {
        try {
            CustomerLedgerPrintDTO data = ledgerService.getLedgerPrintData(customerId);
            String safeCode = data.getCustomerCode() != null ? data.getCustomerCode() : String.valueOf(customerId);

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customer_ledger_" + safeCode + ".csv");

            String csvContent = ledgerService.generateLedgerCsv(customerId);
            response.getOutputStream().write(csvContent.getBytes(StandardCharsets.UTF_8));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
