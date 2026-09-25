package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.DriverPayrollCreateDTO;
import com.transport.erp.dto.DriverPayrollPaymentDTO;
import com.transport.erp.model.DriverPayroll;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.DriverPayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/driver-payrolls")
@CrossOrigin(origins = "*")
public class DriverPayrollController {

    @Autowired
    private DriverPayrollService payrollService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<Page<DriverPayroll>> getPayrolls(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Integer payYear,
            @RequestParam(required = false) Integer payMonth,
            Pageable pageable) {
        Page<DriverPayroll> payrolls = payrollService.searchPayrolls(companyId, driverId, payYear, payMonth, status, pageable);
        return ApiResponse.success(payrolls, "Driver payrolls retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<DriverPayroll> getPayrollById(@PathVariable Long id) {
        DriverPayroll payroll = payrollService.getPayrollById(id);
        return ApiResponse.success(payroll, "Driver payroll retrieved successfully");
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<List<DriverPayroll>> getPayrollsByDriver(@PathVariable Long driverId) {
        List<DriverPayroll> payrolls = payrollService.getPayrollsByDriver(driverId);
        return ApiResponse.success(payrolls, "Driver payrolls retrieved successfully");
    }

    /** Driver self-service: own POSTED / PAID payrolls. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<DriverPayroll>> getMyPayrolls() {
        return ApiResponse.success(payrollService.getMyPayrolls(), "Your salary records");
    }

    @GetMapping("/my/{id}/print")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<com.transport.erp.dto.DriverPayrollPrintDTO> getMySalarySlip(@PathVariable Long id) {
        return ApiResponse.success(payrollService.getMySalarySlip(id), "Your salary slip");
    }

    @GetMapping("/my/{id}/pdf")
    @PreAuthorize("hasRole('DRIVER')")
    public void downloadMySalarySlipPdf(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=\"Salary_Slip_" + id + ".pdf\"");
        payrollService.generateMySalarySlipPdf(id, response.getOutputStream());
        response.getOutputStream().flush();
    }

    @PostMapping({"", "/generate"})
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<DriverPayroll> createPayroll(@RequestBody DriverPayrollCreateDTO dto, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        DriverPayroll created = payrollService.createPayroll(dto, username);
        return ApiResponse.success(created, "Driver payroll DRAFT generated from completed trips");
    }

    @PostMapping("/{id}/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<DriverPayroll> recalculatePayroll(@PathVariable Long id, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        return ApiResponse.success(payrollService.recalculatePayroll(id, username), "Driver payroll recalculated from trips");
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'ACCOUNTANT')")
    public ApiResponse<DriverPayroll> postPayroll(@PathVariable Long id, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        return ApiResponse.success(payrollService.postPayroll(id, username), "Driver payroll posted to accounts");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<DriverPayroll> updatePayroll(@PathVariable Long id, @RequestBody DriverPayrollCreateDTO dto, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        DriverPayroll updated = payrollService.updatePayroll(id, dto, username);
        return ApiResponse.success(updated, "Driver payroll updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<Void> deletePayroll(@PathVariable Long id, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        payrollService.deletePayroll(id, username);
        return ApiResponse.success(null, "Driver payroll deleted successfully");
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<DriverPayroll> approvePayroll(@PathVariable Long id, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        DriverPayroll approved = payrollService.approvePayroll(id, username);
        return ApiResponse.success(approved, "Driver payroll approved. Post it to create the accounting entries.");
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<DriverPayroll> payPayroll(@PathVariable Long id, @RequestBody(required = false) DriverPayrollPaymentDTO paymentDto, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        DriverPayroll paid = payrollService.payPayroll(id, paymentDto, username);
        return ApiResponse.success(paid, "Driver salary paid and payment JV posted");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER')")
    public ApiResponse<DriverPayroll> cancelPayroll(@PathVariable Long id, Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        DriverPayroll cancelled = payrollService.cancelPayroll(id, username);
        return ApiResponse.success(cancelled, "Driver payroll cancelled and reversal JVs posted");
    }

    @GetMapping("/{id}/print")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public ApiResponse<com.transport.erp.dto.DriverPayrollPrintDTO> getSalarySlipPrintData(@PathVariable Long id) {
        com.transport.erp.dto.DriverPayrollPrintDTO printData = payrollService.getSalarySlipPrintData(id);
        return ApiResponse.success(printData, "Driver salary slip print data fetched successfully");
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public void downloadSalarySlipPdf(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        com.transport.erp.dto.DriverPayrollPrintDTO printData = payrollService.getSalarySlipPrintData(id);
        response.setContentType("application/pdf");
        String filename = "Salary_Slip_" + (printData.getPayrollNumber() != null ? printData.getPayrollNumber() : id) + ".pdf";
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        payrollService.generateSalarySlipPdf(id, response.getOutputStream());
        response.getOutputStream().flush();
    }

    @Autowired
    private com.transport.erp.service.XlsxExportService xlsxExportService;

    @GetMapping({"/export/xlsx", "/xlsx"})
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'ACCOUNTANT')")
    public void exportXlsx(@RequestParam(required = false) Long companyId, jakarta.servlet.http.HttpServletResponse response) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            byte[] bytes = xlsxExportService.exportDriverPayroll(scopedCompanyId);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=driver_payrolls_export.xlsx");
            response.getOutputStream().write(bytes);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
