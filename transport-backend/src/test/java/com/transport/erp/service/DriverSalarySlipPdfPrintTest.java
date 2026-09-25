package com.transport.erp.service;

import com.transport.erp.dto.DriverPayrollPrintDTO;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.Company;
import com.transport.erp.model.Driver;
import com.transport.erp.model.DriverPayroll;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.DriverPayrollRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DriverSalarySlipPdfPrintTest {

    @Mock
    private DriverPayrollRepository payrollRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private TenantAccessService tenantAccess;

    @InjectMocks
    private DriverPayrollService payrollService;

    private DriverPayroll samplePayroll;
    private Driver sampleDriver;
    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        com.transport.erp.model.AppUser admin = new com.transport.erp.model.AppUser();
        admin.setId(1L);
        admin.setCompanyId(1L);
        org.mockito.Mockito.lenient().when(tenantAccess.requireCurrentUser()).thenReturn(admin);
        org.mockito.Mockito.lenient().when(tenantAccess.isSuperAdmin(org.mockito.ArgumentMatchers.any(com.transport.erp.model.AppUser.class))).thenReturn(true);
        sampleDriver = new Driver();
        sampleDriver.setId(10L);
        sampleDriver.setName("Muthu Kumar");
        sampleDriver.setCode("DRV-001");
        sampleDriver.setPhoneNumber("9876543210");
        sampleDriver.setLicenseNumber("TN-38-2020-00123");

        sampleCompany = new Company();
        sampleCompany.setId(1L);
        sampleCompany.setName("TransaFlow Logistics Pvt Ltd");
        sampleCompany.setAddress("123 Transport Hub, Chennai");
        sampleCompany.setPhone("044-22334455");
        sampleCompany.setGstNumber("33AAAAA0000A1Z5");

        samplePayroll = new DriverPayroll();
        samplePayroll.setId(100L);
        samplePayroll.setPayrollNumber("PR-2026-01-0001");
        samplePayroll.setDriver(sampleDriver);
        samplePayroll.setPayYear(2026);
        samplePayroll.setPayMonth(1);
        samplePayroll.setBasicSalary(new BigDecimal("20000.00"));
        samplePayroll.setAllowanceAmount(new BigDecimal("3000.00"));
        samplePayroll.setDeductionAmount(new BigDecimal("1000.00"));
        samplePayroll.setAdvanceAdjustment(new BigDecimal("2000.00"));
        samplePayroll.setNetSalaryPayable(new BigDecimal("20000.00")); // 20000 + 3000 - 1000 - 2000 = 20000
        samplePayroll.setStatus("PAID");
        samplePayroll.setPaymentMethod("BANK_TRANSFER");
        samplePayroll.setAccrualJvNumber("JV-SAL-ACC-100");
        samplePayroll.setPaymentJvNumber("JV-SAL-PAY-100");
        samplePayroll.setCompanyId(1L);
        samplePayroll.setIsDeleted(false);
        samplePayroll.setCreatedDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Get Salary Slip Print Data returns correct DTO values")
    void testGetSalarySlipPrintDataSuccess() {
        when(payrollRepository.findById(100L)).thenReturn(Optional.of(samplePayroll));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        doNothing().when(tenantAccess).assertOwned(1L);

        DriverPayrollPrintDTO dto = payrollService.getSalarySlipPrintData(100L);

        assertNotNull(dto);
        assertEquals("PR-2026-01-0001", dto.getPayrollNumber());
        assertEquals("Muthu Kumar", dto.getDriverName());
        assertEquals("DRV-001", dto.getDriverCode());
        assertEquals("TransaFlow Logistics Pvt Ltd", dto.getCompanyName());
        assertEquals(new BigDecimal("20000.00"), dto.getBasicSalary());
        assertEquals(new BigDecimal("3000.00"), dto.getAllowanceAmount());
        assertEquals(new BigDecimal("23000.00"), dto.getGrossEarnings());
        assertEquals(new BigDecimal("3000.00"), dto.getTotalDeductions());
        assertEquals(new BigDecimal("20000.00"), dto.getNetSalaryPayable());
        assertEquals("PAID", dto.getStatus());

        verify(tenantAccess, times(1)).assertOwned(1L);
        verify(payrollRepository, never()).save(any());
    }

    @Test
    @DisplayName("Generate Salary Slip PDF produces valid PDF byte stream starting with %PDF-")
    void testGenerateSalarySlipPdfSuccess() throws Exception {
        when(payrollRepository.findById(100L)).thenReturn(Optional.of(samplePayroll));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        doNothing().when(tenantAccess).assertOwned(1L);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        payrollService.generateSalarySlipPdf(100L, baos);

        byte[] pdfBytes = baos.toByteArray();
        assertTrue(pdfBytes.length > 0, "PDF output stream should not be empty");

        String pdfHeader = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", pdfHeader, "Generated PDF must start with '%PDF-' signature");
    }

    @Test
    @DisplayName("Generate Salary Slip PDF for DRAFT payroll produces valid PDF stream")
    void testGenerateSalarySlipPdfDraftStatus() throws Exception {
        samplePayroll.setStatus("DRAFT");
        when(payrollRepository.findById(100L)).thenReturn(Optional.of(samplePayroll));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        doNothing().when(tenantAccess).assertOwned(1L);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        payrollService.generateSalarySlipPdf(100L, baos);

        byte[] pdfBytes = baos.toByteArray();
        assertTrue(pdfBytes.length > 0);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    @DisplayName("Cross-tenant access throws AccessDeniedException and denies PDF generation")
    void testTenantAccessDenied() {
        when(payrollRepository.findById(100L)).thenReturn(Optional.of(samplePayroll));
        doThrow(new AccessDeniedException("Access Denied: Cross-tenant access forbidden")).when(tenantAccess).assertOwned(1L);

        assertThrows(AccessDeniedException.class, () -> payrollService.getSalarySlipPrintData(100L));
        assertThrows(AccessDeniedException.class, () -> payrollService.generateSalarySlipPdf(100L, new ByteArrayOutputStream()));
    }

    @Test
    @DisplayName("Nonexistent payroll ID throws BusinessValidationException")
    void testPayrollNotFound() {
        when(payrollRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> payrollService.getSalarySlipPrintData(999L));
    }

    @Test
    @DisplayName("PDF and Print data generation causes zero database state mutations")
    void testZeroStateMutations() throws Exception {
        when(payrollRepository.findById(100L)).thenReturn(Optional.of(samplePayroll));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));

        payrollService.getSalarySlipPrintData(100L);
        payrollService.generateSalarySlipPdf(100L, new ByteArrayOutputStream());

        verify(payrollRepository, never()).save(any());
        verify(payrollRepository, never()).delete(any());
    }
}
