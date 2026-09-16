package com.transport.erp.service;

import com.transport.erp.dto.CustomerLedgerPrintDTO;
import com.transport.erp.model.*;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.CustomerLedgerRepository;
import com.transport.erp.repository.CustomerRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerLedgerPdfCsvPrintTest {

    @Mock
    private CustomerLedgerRepository ledgerRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private TenantAccessService tenantAccess;

    private CustomerLedgerService ledgerService;

    private AppUser mockUser;
    private Customer mockCustomer;
    private Company mockCompany;
    private Branch mockBranch;
    private List<CustomerLedger> mockEntries;

    @BeforeEach
    void setUp() {
        ledgerService = new CustomerLedgerService();
        ReflectionTestUtils.setField(ledgerService, "ledgerRepository", ledgerRepository);
        ReflectionTestUtils.setField(ledgerService, "customerRepository", customerRepository);
        ReflectionTestUtils.setField(ledgerService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(ledgerService, "branchRepository", branchRepository);
        ReflectionTestUtils.setField(ledgerService, "tenantAccess", tenantAccess);

        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setUsername("test_user");
        mockUser.setCompanyId(1L);
        mockUser.setBranchId(10L);

        mockCompany = new Company();
        mockCompany.setId(1L);
        mockCompany.setName("TransaFlow Logistics Pvt Ltd");
        mockCompany.setAddress("100 Industrial Estate, Chennai");
        mockCompany.setPhone("044-24567890");
        mockCompany.setEmail("billing@transaflow.com");
        mockCompany.setGstNumber("33AAAAA0000A1Z5");
        mockCompany.setPanNumber("AAAAA0000A");

        mockBranch = new Branch();
        mockBranch.setId(10L);
        mockBranch.setName("Chennai Port Branch");
        mockBranch.setAddress("Port Terminal Road, Royapuram");
        mockBranch.setPhone("044-24567899");

        mockCustomer = new Customer();
        mockCustomer.setId(100L);
        mockCustomer.setName("Acme Freight Infrastructure, Inc.");
        mockCustomer.setCode("CUST-ACME-100");
        mockCustomer.setAddress("78 Harbour Highway, Tuticorin");
        mockCustomer.setPhone("9840012345");
        mockCustomer.setEmail("accounts@acmefreight.com");
        mockCustomer.setGstNumber("33ACMEE1234F1Z9");
        mockCustomer.setCompanyId(1L);
        mockCustomer.setBranchId(10L);

        SalesInvoice mockInvoice = new SalesInvoice();
        mockInvoice.setId(500L);
        mockInvoice.setInvoiceNumber("INV-2026-0500");

        CustomerReceipt mockReceipt = new CustomerReceipt();
        mockReceipt.setId(600L);
        mockReceipt.setReceiptNumber("REC-2026-0600");

        CustomerLedger entry1 = new CustomerLedger();
        entry1.setId(1001L);
        entry1.setCustomer(mockCustomer);
        entry1.setInvoice(mockInvoice);
        entry1.setDebitAmount(new BigDecimal("10000.00"));
        entry1.setCreditAmount(BigDecimal.ZERO);
        entry1.setRunningBalance(new BigDecimal("10000.00"));
        entry1.setRemarks("Sales invoice entry INV-2026-0500");
        entry1.setCreatedDate(LocalDateTime.of(2026, 9, 10, 10, 0));

        CustomerLedger entry2 = new CustomerLedger();
        entry2.setId(1002L);
        entry2.setCustomer(mockCustomer);
        entry2.setReceipt(mockReceipt);
        entry2.setDebitAmount(BigDecimal.ZERO);
        entry2.setCreditAmount(new BigDecimal("4000.00"));
        entry2.setRunningBalance(new BigDecimal("6000.00"));
        entry2.setRemarks("Payment received via receipt REC-2026-0600");
        entry2.setCreatedDate(LocalDateTime.of(2026, 9, 12, 14, 30));

        mockEntries = new ArrayList<>();
        mockEntries.add(entry1);
        mockEntries.add(entry2);
    }

    @Test
    @DisplayName("1. getLedgerPrintData — Correctly populates customer, company, and ledger entries using authoritative DB records")
    void getLedgerPrintData_ValidCustomer_PopulatesLedgerData() {
        when(customerRepository.findById(100L)).thenReturn(Optional.of(mockCustomer));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));
        when(ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(100L)).thenReturn(mockEntries);

        CustomerLedgerPrintDTO dto = ledgerService.getLedgerPrintData(100L);

        assertNotNull(dto);
        assertEquals(100L, dto.getCustomerId());
        assertEquals("Acme Freight Infrastructure, Inc.", dto.getCustomerName());
        assertEquals("CUST-ACME-100", dto.getCustomerCode());
        assertEquals("33ACMEE1234F1Z9", dto.getCustomerGSTIN());
        assertEquals("TransaFlow Logistics Pvt Ltd", dto.getCompanyName());
        assertEquals("Chennai Port Branch", dto.getBranchName());

        assertEquals(new BigDecimal("10000.00"), dto.getTotalDebit());
        assertEquals(new BigDecimal("4000.00"), dto.getTotalCredit());
        assertEquals(new BigDecimal("6000.00"), dto.getClosingBalance());
        assertEquals(2, dto.getItems().size());
        assertEquals("Sales Invoice", dto.getItems().get(0).getTransactionType());
        assertEquals("INV-2026-0500", dto.getItems().get(0).getReferenceNumber());
        assertEquals("Customer Receipt", dto.getItems().get(1).getTransactionType());
        assertEquals("REC-2026-0600", dto.getItems().get(1).getReferenceNumber());
    }

    @Test
    @DisplayName("2. generateLedgerPdf — Streams valid PDF bytes starting with %PDF- header")
    void generateLedgerPdf_ValidCustomer_StreamsValidPdfBytes() throws Exception {
        when(customerRepository.findById(100L)).thenReturn(Optional.of(mockCustomer));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));
        when(ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(100L)).thenReturn(mockEntries);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ledgerService.generateLedgerPdf(100L, baos);

        byte[] pdfBytes = baos.toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF byte stream should not be empty");

        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", pdfHeader, "Generated stream must be a valid PDF starting with %PDF-");
    }

    @Test
    @DisplayName("3. generateLedgerCsv — Formats correct CSV text with escaping")
    void generateLedgerCsv_ValidCustomer_FormatsCorrectCsvWithEscaping() {
        when(customerRepository.findById(100L)).thenReturn(Optional.of(mockCustomer));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));
        when(ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(100L)).thenReturn(mockEntries);

        String csv = ledgerService.generateLedgerCsv(100L);

        assertNotNull(csv);
        assertTrue(csv.contains("Customer Ledger Statement for \"Acme Freight Infrastructure, Inc.\""));
        assertTrue(csv.contains("Date & Time,Transaction Type,Reference Number,Remarks / Description,Debit,Credit,Running Balance"));
        assertTrue(csv.contains("Sales Invoice,INV-2026-0500"));
        assertTrue(csv.contains("Customer Receipt,REC-2026-0600"));
        assertTrue(csv.contains("10000.00,0,10000.00"));
        assertTrue(csv.contains("0,4000.00,6000.00"));
    }

    @Test
    @DisplayName("4. getLedgerPrintData — Cross-company access throws AccessDeniedException")
    void getLedgerPrintData_CrossCompanyAccess_ThrowsAccessDeniedException() {
        when(customerRepository.findById(100L)).thenReturn(Optional.of(mockCustomer));
        doThrow(new AccessDeniedException("Access denied: Company mismatch"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () -> ledgerService.getLedgerPrintData(100L));
    }

    @Test
    @DisplayName("5. getLedgerPrintData — Cross-branch access throws AccessDeniedException")
    void getLedgerPrintData_CrossBranchAccess_ThrowsAccessDeniedException() {
        AppUser otherBranchUser = new AppUser();
        otherBranchUser.setId(2L);
        otherBranchUser.setCompanyId(1L);
        otherBranchUser.setBranchId(99L); // Different branch

        when(customerRepository.findById(100L)).thenReturn(Optional.of(mockCustomer));
        when(tenantAccess.requireCurrentUser()).thenReturn(otherBranchUser);
        when(tenantAccess.isSuperAdmin(otherBranchUser)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> ledgerService.getLedgerPrintData(100L));
    }

    @Test
    @DisplayName("6. getLedgerPrintData — Non-existent customer throws IllegalArgumentException")
    void getLedgerPrintData_NonExistentCustomer_ThrowsIllegalArgumentException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> ledgerService.getLedgerPrintData(999L));
    }

    @Test
    @DisplayName("7. generateLedgerExports — Strictly 0 database or accounting mutations during export")
    void generateLedgerExports_ZeroDatabaseMutations() throws Exception {
        when(customerRepository.findById(100L)).thenReturn(Optional.of(mockCustomer));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));
        when(ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(100L)).thenReturn(mockEntries);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ledgerService.generateLedgerPdf(100L, baos);
        ledgerService.generateLedgerCsv(100L);

        verify(ledgerRepository, never()).save(any());
        verify(customerRepository, never()).save(any());
    }
}
