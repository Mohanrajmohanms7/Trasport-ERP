package com.transport.erp.service;

import com.transport.erp.dto.*;
import com.transport.erp.model.*;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.ChartOfAccountRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancialReportPdfCsvPrintTest {

    @Mock
    private JournalVoucherRepository jvRepository;

    @Mock
    private ChartOfAccountRepository coaRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private TenantAccessService tenantAccess;

    private FinancialReportService reportService;

    private Company mockCompany;
    private Branch mockBranch;
    private AppUser mockUser;
    private ChartOfAccount mockAccount1; // Asset
    private ChartOfAccount mockAccount2; // Expense
    private ChartOfAccount mockAccount3; // Income

    @BeforeEach
    void setUp() {
        reportService = new FinancialReportService();
        ReflectionTestUtils.setField(reportService, "jvRepository", jvRepository);
        ReflectionTestUtils.setField(reportService, "coaRepository", coaRepository);
        ReflectionTestUtils.setField(reportService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(reportService, "branchRepository", branchRepository);
        ReflectionTestUtils.setField(reportService, "tenantAccess", tenantAccess);

        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setUsername("test_admin");
        mockUser.setCompanyId(1L);
        mockUser.setBranchId(10L);

        mockCompany = new Company();
        mockCompany.setId(1L);
        mockCompany.setName("TransaFlow Transport Solutions Ltd");
        mockCompany.setAddress("45 Corporate Tower, Chennai");
        mockCompany.setPhone("044-23456789");
        mockCompany.setEmail("finance@transaflow.com");
        mockCompany.setGstNumber("33AAAAA1234A1Z5");
        mockCompany.setPanNumber("AAAAA1234A");

        mockBranch = new Branch();
        mockBranch.setId(10L);
        mockBranch.setName("Main Hub Branch");

        mockAccount1 = new ChartOfAccount();
        mockAccount1.setId(101L);
        mockAccount1.setCompanyId(1L);
        mockAccount1.setAccountCode("1001");
        mockAccount1.setAccountName("Bank Account - HDFC");
        mockAccount1.setAccountType("ASSET");
        mockAccount1.setOpeningBalance(new BigDecimal("50000.00"));

        mockAccount2 = new ChartOfAccount();
        mockAccount2.setId(201L);
        mockAccount2.setCompanyId(1L);
        mockAccount2.setAccountCode("5001");
        mockAccount2.setAccountName("Fuel Expense Account");
        mockAccount2.setAccountType("EXPENSE");
        mockAccount2.setOpeningBalance(BigDecimal.ZERO);

        mockAccount3 = new ChartOfAccount();
        mockAccount3.setId(301L);
        mockAccount3.setCompanyId(1L);
        mockAccount3.setAccountCode("4001");
        mockAccount3.setAccountName("Freight Revenue Account");
        mockAccount3.setAccountType("INCOME");
        mockAccount3.setOpeningBalance(BigDecimal.ZERO);

        List<ChartOfAccount> accounts = List.of(mockAccount1, mockAccount2, mockAccount3);

        when(tenantAccess.resolveCompanyId(1L)).thenReturn(1L);
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));
        when(coaRepository.findByCompanyIdAndIsDeletedFalseOrderByAccountCodeAsc(1L)).thenReturn(accounts);
        when(coaRepository.findById(101L)).thenReturn(Optional.of(mockAccount1));

        // Mock JV aggregations
        when(jvRepository.sumDebitByAccountAndCompanyBeforeDate(anyLong(), anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(jvRepository.sumCreditByAccountAndCompanyBeforeDate(anyLong(), anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(jvRepository.sumDebitByAccountAndCompanyAndDateRange(eq(101L), anyLong(), any(), any())).thenReturn(new BigDecimal("20000.00"));
        when(jvRepository.sumCreditByAccountAndCompanyAndDateRange(eq(101L), anyLong(), any(), any())).thenReturn(new BigDecimal("5000.00"));
        when(jvRepository.sumDebitByAccountAndCompanyAndDateRange(eq(201L), anyLong(), any(), any())).thenReturn(new BigDecimal("10000.00"));
        when(jvRepository.sumCreditByAccountAndCompanyAndDateRange(eq(201L), anyLong(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(jvRepository.sumDebitByAccountAndCompanyAndDateRange(eq(301L), anyLong(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(jvRepository.sumCreditByAccountAndCompanyAndDateRange(eq(301L), anyLong(), any(), any())).thenReturn(new BigDecimal("25000.00"));

        when(jvRepository.sumDebitByAccountAndCompanyOnOrBeforeDate(eq(101L), anyLong(), any())).thenReturn(new BigDecimal("20000.00"));
        when(jvRepository.sumCreditByAccountAndCompanyOnOrBeforeDate(eq(101L), anyLong(), any())).thenReturn(new BigDecimal("5000.00"));
        when(jvRepository.sumDebitByAccountAndCompanyOnOrBeforeDate(eq(201L), anyLong(), any())).thenReturn(new BigDecimal("10000.00"));
        when(jvRepository.sumCreditByAccountAndCompanyOnOrBeforeDate(eq(201L), anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(jvRepository.sumDebitByAccountAndCompanyOnOrBeforeDate(eq(301L), anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(jvRepository.sumCreditByAccountAndCompanyOnOrBeforeDate(eq(301L), anyLong(), any())).thenReturn(new BigDecimal("25000.00"));

        when(jvRepository.findGeneralLedgerEntries(eq(101L), anyLong(), any(), any())).thenReturn(new ArrayList<>());
    }

    // ==========================================
    // 1. TRIAL BALANCE TESTS
    // ==========================================
    @Test
    @DisplayName("1. Trial Balance PDF — Streams valid PDF bytes starting with %PDF-")
    void trialBalancePdf_GeneratesValidPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateTrialBalancePdf(1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), baos);

        byte[] bytes = baos.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        String header = new String(bytes, 0, Math.min(bytes.length, 5));
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("2. Trial Balance CSV — Formats correct UTF-8 CSV string with totals")
    void trialBalanceCsv_FormatsValidCsv() {
        String csv = reportService.generateTrialBalanceCsv(1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertNotNull(csv);
        assertTrue(csv.contains("TRIAL BALANCE STATEMENT"));
        assertTrue(csv.contains("TransaFlow Transport Solutions Ltd"));
        assertTrue(csv.contains("Account Code,Account Name,Account Type"));
        assertTrue(csv.contains("1001,Bank Account - HDFC,ASSET"));
        assertTrue(csv.contains("TOTALS"));
    }

    @Test
    @DisplayName("3. Trial Balance Print — Populates company header & report data")
    void trialBalancePrint_PopulatesData() {
        FinancialReportPrintDTO<TrialBalanceDTO.Response> printDTO = reportService.getTrialBalancePrintData(1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertNotNull(printDTO);
        assertEquals("TransaFlow Transport Solutions Ltd", printDTO.getCompanyName());
        assertEquals("Main Hub Branch", printDTO.getBranchName());
        assertNotNull(printDTO.getReportData());
        assertEquals(3, printDTO.getReportData().getRows().size());
    }

    // ==========================================
    // 2. GENERAL LEDGER TESTS
    // ==========================================
    @Test
    @DisplayName("4. General Ledger PDF — Streams valid PDF bytes starting with %PDF-")
    void generalLedgerPdf_GeneratesValidPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateGeneralLedgerPdf(101L, 1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), baos);

        byte[] bytes = baos.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        String header = new String(bytes, 0, Math.min(bytes.length, 5));
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("5. General Ledger CSV — Formats valid CSV with account details")
    void generalLedgerCsv_FormatsValidCsv() {
        String csv = reportService.generateGeneralLedgerCsv(101L, 1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertNotNull(csv);
        assertTrue(csv.contains("GENERAL LEDGER STATEMENT"));
        assertTrue(csv.contains("1001 - Bank Account - HDFC (ASSET)"));
        assertTrue(csv.contains("Voucher Date,Voucher Number,Reference Number"));
    }

    // ==========================================
    // 3. PROFIT & LOSS TESTS
    // ==========================================
    @Test
    @DisplayName("6. Profit & Loss PDF — Streams valid PDF bytes starting with %PDF-")
    void profitLossPdf_GeneratesValidPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateProfitLossPdf(1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), baos);

        byte[] bytes = baos.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        String header = new String(bytes, 0, Math.min(bytes.length, 5));
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("7. Profit & Loss CSV — Correctly reconciles income, expenses, and net profit")
    void profitLossCsv_FormatsValidCsv() {
        String csv = reportService.generateProfitLossCsv(1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertNotNull(csv);
        assertTrue(csv.contains("PROFIT & LOSS STATEMENT"));
        assertTrue(csv.contains("Total Income: 25000.00"));
        assertTrue(csv.contains("Total Expenses: 10000.00"));
        assertTrue(csv.contains("INCOME,4001,Freight Revenue Account"));
        assertTrue(csv.contains("EXPENSE,5001,Fuel Expense Account"));
    }

    // ==========================================
    // 4. BALANCE SHEET TESTS
    // ==========================================
    @Test
    @DisplayName("8. Balance Sheet PDF — Streams valid PDF bytes starting with %PDF-")
    void balanceSheetPdf_GeneratesValidPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateBalanceSheetPdf(1L, LocalDate.of(2026, 9, 30), baos);

        byte[] bytes = baos.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        String header = new String(bytes, 0, Math.min(bytes.length, 5));
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("9. Balance Sheet CSV — Formats valid CSV with Assets, Liabilities, Equity")
    void balanceSheetCsv_FormatsValidCsv() {
        String csv = reportService.generateBalanceSheetCsv(1L, LocalDate.of(2026, 9, 30));

        assertNotNull(csv);
        assertTrue(csv.contains("BALANCE SHEET STATEMENT"));
        assertTrue(csv.contains("ASSETS,1001,Bank Account - HDFC"));
        assertTrue(csv.contains("Total Assets: 65000.00"));
    }

    // ==========================================
    // 5. SECURITY & ZERO MUTATION TESTS
    // ==========================================
    @Test
    @DisplayName("10. Company Isolation — AccessDeniedException when accessing unauthorized company")
    void crossCompanyAccess_ThrowsAccessDeniedException() {
        doThrow(new AccessDeniedException("Access denied for company 999"))
                .when(tenantAccess).assertCompanyAccess(999L);
        when(coaRepository.findById(99901L)).thenReturn(Optional.of(new ChartOfAccount() {{ setCompanyId(999L); }}));

        assertThrows(AccessDeniedException.class, () -> reportService.getGeneralLedger(99901L, 999L, null, null));
    }

    @Test
    @DisplayName("11. Zero DB Mutations — Export operations perform zero saves/deletes")
    void financialReportExports_ZeroDatabaseMutations() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateTrialBalancePdf(1L, null, null, baos);
        reportService.generateGeneralLedgerPdf(101L, 1L, null, null, baos);
        reportService.generateProfitLossPdf(1L, null, null, baos);
        reportService.generateBalanceSheetPdf(1L, null, baos);

        reportService.generateTrialBalanceCsv(1L, null, null);
        reportService.generateGeneralLedgerCsv(101L, 1L, null, null);
        reportService.generateProfitLossCsv(1L, null, null);
        reportService.generateBalanceSheetCsv(1L, null);

        verify(coaRepository, never()).save(any());
        verify(jvRepository, never()).save(any());
        verify(companyRepository, never()).save(any());
    }
}
