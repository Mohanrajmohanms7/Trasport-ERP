package com.transport.erp.controller;

import com.transport.erp.service.XlsxExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * One export endpoint for every list screen: GET /api/v1/exports/{module}?format=xlsx|pdf.
 * Always company-scoped; finance lists need an accounts/admin role.
 */
@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {

    @Autowired
    private XlsxExportService exports;

    private static final Set<String> FINANCE = Set.of("invoices", "receipts", "payroll", "driver-advances", "journal", "chart-of-accounts", "expenses");
    private static final Set<String> FINANCE_ROLES = Set.of("ROLE_SUPER_ADMIN", "ROLE_COMPANY_ADMIN", "ROLE_ADMIN", "ROLE_ACCOUNTANT", "ROLE_BRANCH_MANAGER", "ROLE_VIEWER");

    private Map<String, BiFunction<Long, String, byte[]>> registry() {
        return Map.ofEntries(
                Map.entry("materials", exports::exportMaterials),
                Map.entry("vehicles", exports::exportVehicles),
                Map.entry("drivers", exports::exportDrivers),
                Map.entry("customers", exports::exportCustomers),
                Map.entry("bookings", exports::exportBookings),
                Map.entry("trips", exports::exportTrips),
                Map.entry("fuel", exports::exportFuelEntries),
                Map.entry("expenses", exports::exportExpenses),
                Map.entry("payroll", exports::exportDriverPayroll),
                Map.entry("invoices", exports::exportSalesInvoices),
                Map.entry("receipts", exports::exportCustomerReceipts),
                Map.entry("work-orders", exports::exportWorkOrders),
                Map.entry("maintenance-requests", exports::exportMaintenanceRequests),
                Map.entry("spare-parts", exports::exportSpareParts),
                Map.entry("stock", exports::exportStock),
                Map.entry("inventory-transactions", exports::exportInventoryTransactions),
                Map.entry("driver-advances", exports::exportDriverAdvances),
                Map.entry("journal", exports::exportJournal),
                Map.entry("chart-of-accounts", exports::exportChartOfAccounts),
                Map.entry("suppliers", exports::exportSuppliers)
        );
    }

    @GetMapping("/{module}")
    public ResponseEntity<byte[]> export(@PathVariable String module,
                                         @RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam(required = false) Long companyId) {
        BiFunction<Long, String, byte[]> fn = registry().get(module);
        if (fn == null) {
            return ResponseEntity.notFound().build();
        }
        if (FINANCE.contains(module)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean allowed = auth != null && auth.getAuthorities().stream().anyMatch(a -> FINANCE_ROLES.contains(a.getAuthority()));
            if (!allowed) throw new AccessDeniedException("Finance exports need an accounts or admin role.");
        }
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] body = fn.apply(companyId, pdf ? "pdf" : "xlsx");
        String file = module + "_" + LocalDate.now() + (pdf ? ".pdf" : ".xlsx");
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(body);
    }
}
