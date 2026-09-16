package com.transport.erp.service;

import com.transport.erp.dto.SalesInvoicePrintDTO;
import com.transport.erp.model.*;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.repository.SalesInvoiceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesInvoicePdfPrintTest {

    @Mock
    private SalesInvoiceRepository invoiceRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private JournalVoucherRepository jvRepository;

    @Mock
    private TenantAccessService tenantAccess;

    private SalesInvoiceService invoiceService;

    private AppUser mockUser;
    private SalesInvoice mockInvoice;
    private Company mockCompany;
    private Branch mockBranch;
    private Customer mockCustomer;
    private Material mockMaterial;
    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        invoiceService = new SalesInvoiceService();
        ReflectionTestUtils.setField(invoiceService, "invoiceRepository", invoiceRepository);
        ReflectionTestUtils.setField(invoiceService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(invoiceService, "branchRepository", branchRepository);
        ReflectionTestUtils.setField(invoiceService, "jvRepository", jvRepository);
        ReflectionTestUtils.setField(invoiceService, "tenantAccess", tenantAccess);

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
        mockCustomer.setName("Southern Freight Corproation");
        mockCustomer.setCode("CUST-100");
        mockCustomer.setAddress("45 Anna Salai, Chennai");
        mockCustomer.setPhone("9876543210");
        mockCustomer.setEmail("finance@southernfreight.com");
        mockCustomer.setGstNumber("33BBBCC1111B1Z2");

        mockMaterial = new Material();
        mockMaterial.setId(5L);
        mockMaterial.setName("Blue Metal 20mm");

        mockTrip = new Trip();
        mockTrip.setId(50L);
        mockTrip.setTripNumber("TRIP-2026-050");

        SalesInvoiceDetail detail = new SalesInvoiceDetail();
        detail.setId(1001L);
        detail.setTrip(mockTrip);
        detail.setMaterial(mockMaterial);
        detail.setQuantity(new BigDecimal("20.00"));
        detail.setRate(new BigDecimal("800.00"));
        detail.setFreightCharges(new BigDecimal("100.00"));
        detail.setLoadingCharges(new BigDecimal("20.00"));
        detail.setRoyalty(new BigDecimal("10.00"));
        detail.setGstPercentage(new BigDecimal("18.00"));
        detail.setCgst(new BigDecimal("1674.00")); // (20 * 930) * 0.09 = 1674
        detail.setSgst(new BigDecimal("1674.00"));
        detail.setIgst(BigDecimal.ZERO);
        detail.setNetAmount(new BigDecimal("21948.00")); // (20 * 930) + 3348 = 21948

        mockInvoice = new SalesInvoice();
        mockInvoice.setId(200L);
        mockInvoice.setInvoiceNumber("INV-2026-0200");
        mockInvoice.setInvoiceDate(LocalDate.of(2026, 9, 15));
        mockInvoice.setStatus("APPROVED");
        mockInvoice.setPaymentTerms("NET_30");
        mockInvoice.setPaymentStatus("UNPAID");
        mockInvoice.setSubtotal(new BigDecimal("21948.00"));
        mockInvoice.setDiscount(new BigDecimal("500.00"));
        mockInvoice.setNetAmount(new BigDecimal("21448.00"));
        mockInvoice.setPaidAmount(BigDecimal.ZERO);
        mockInvoice.setCompanyId(1L);
        mockInvoice.setBranchId(10L);
        mockInvoice.setCustomer(mockCustomer);
        mockInvoice.setIsDeleted(false);

        List<SalesInvoiceDetail> detailsList = new ArrayList<>();
        detailsList.add(detail);
        mockInvoice.setDetails(detailsList);
    }

    @Test
    @DisplayName("1. generateInvoicePdf — Streams valid PDF bytes matching %PDF- header")
    void generateInvoicePdf_ValidInvoice_StreamsValidPdfBytes() throws Exception {
        when(invoiceRepository.findById(200L)).thenReturn(Optional.of(mockInvoice));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        invoiceService.generateInvoicePdf(200L, baos);

        byte[] pdfBytes = baos.toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF byte stream should not be empty");

        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertEquals("%PDF-", pdfHeader, "Generated stream must be a valid PDF starting with %PDF-");
    }

    @Test
    @DisplayName("2. getInvoicePrintData — Correctly populates invoice, company, customer, and line items")
    void getInvoicePrintData_PopulatesAllFields() {
        when(invoiceRepository.findById(200L)).thenReturn(Optional.of(mockInvoice));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));

        SalesInvoicePrintDTO dto = invoiceService.getInvoicePrintData(200L);

        assertNotNull(dto);
        assertEquals(200L, dto.getInvoiceId());
        assertEquals("INV-2026-0200", dto.getInvoiceNumber());
        assertEquals("APPROVED", dto.getStatus());
        assertEquals("NET_30", dto.getPaymentTerms());

        // Customer
        assertEquals("Southern Freight Corproation", dto.getCustomerName());
        assertEquals("33BBBCC1111B1Z2", dto.getCustomerGSTIN());

        // Company
        assertEquals("TransaFlow Logistics Pvt Ltd", dto.getCompanyName());
        assertEquals("33AAAAA0000A1Z5", dto.getCompanyGSTIN());

        // Branch
        assertEquals("Chennai Port Branch", dto.getBranchName());

        // Financials
        assertEquals(new BigDecimal("21448.00"), dto.getNetAmount());
        assertEquals(new BigDecimal("21448.00"), dto.getBalanceDue());
        assertEquals(1, dto.getItems().size());
        assertEquals("Blue Metal 20mm", dto.getItems().get(0).getMaterialName());
        assertEquals("TRIP-2026-050", dto.getItems().get(0).getTripNumber());
    }

    @Test
    @DisplayName("3. getInvoicePrintData — Cross-branch tenant access throws AccessDeniedException")
    void getInvoicePrintData_CrossBranchAccess_ThrowsAccessDeniedException() {
        AppUser otherBranchUser = new AppUser();
        otherBranchUser.setId(2L);
        otherBranchUser.setCompanyId(1L);
        otherBranchUser.setBranchId(99L); // Different branch

        when(invoiceRepository.findById(200L)).thenReturn(Optional.of(mockInvoice));
        when(tenantAccess.requireCurrentUser()).thenReturn(otherBranchUser);
        when(tenantAccess.isSuperAdmin(otherBranchUser)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> invoiceService.getInvoicePrintData(200L));
    }

    @Test
    @DisplayName("4. generateInvoicePdf — Non-existent invoice throws IllegalArgumentException")
    void generateInvoicePdf_NonExistentInvoice_ThrowsIllegalArgumentException() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> invoiceService.generateInvoicePdf(999L, new ByteArrayOutputStream()));
    }

    @Test
    @DisplayName("5. generateInvoicePdf — Strictly 0 database mutations during PDF generation")
    void generateInvoicePdf_ZeroDatabaseMutations() throws Exception {
        when(invoiceRepository.findById(200L)).thenReturn(Optional.of(mockInvoice));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(mockBranch));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        invoiceService.generateInvoicePdf(200L, baos);

        verify(invoiceRepository, never()).save(any());
        verify(jvRepository, never()).save(any());
    }
}
