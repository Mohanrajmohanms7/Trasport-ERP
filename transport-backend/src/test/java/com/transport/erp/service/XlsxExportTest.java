package com.transport.erp.service;

import com.transport.erp.dto.CustomerLedgerPrintDTO;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class XlsxExportTest {

    @Mock private TenantAccessService tenantAccess;
    @Mock private MaterialRepository materialRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TripRepository tripRepository;
    @Mock private FuelEntryRepository fuelEntryRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private DriverPayrollRepository driverPayrollRepository;
    @Mock private SalesInvoiceRepository salesInvoiceRepository;
    @Mock private CustomerReceiptRepository customerReceiptRepository;
    @Mock private CustomerLedgerService customerLedgerService;
    @Mock private ReportTemplateService reportTemplateService;

    @InjectMocks
    private XlsxExportService xlsxExportService;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(tenantAccess.resolveCompanyId(any())).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("Export Materials - Valid Workbook Structure")
    void testExportMaterials() throws Exception {
        Material m = new Material();
        m.setCode("MAT-001");
        m.setName("Blue Metal 20mm");
        m.setDefaultRate(new BigDecimal("45.50"));
        m.setDensity(new BigDecimal("1.60"));
        m.setStatus("ACTIVE");

        when(materialRepository.findByCompanyIdAndIsDeletedFalse(eq(COMPANY_ID), any()))
                .thenReturn(new PageImpl<>(List.of(m)));

        byte[] bytes = xlsxExportService.exportMaterials(COMPANY_ID);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Materials");
            assertNotNull(sheet);
            Row headerRow = sheet.getRow(0);
            assertEquals("Code", headerRow.getCell(0).getStringCellValue());
            assertEquals("Name", headerRow.getCell(1).getStringCellValue());

            Row dataRow = sheet.getRow(1);
            assertEquals("MAT-001", dataRow.getCell(0).getStringCellValue());
            assertEquals("Blue Metal 20mm", dataRow.getCell(1).getStringCellValue());
        }

        // Verify zero database modifications
        verify(materialRepository, never()).save(any());
        verify(materialRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Export Vehicles - Valid Workbook Structure")
    void testExportVehicles() throws Exception {
        Vehicle v = new Vehicle();
        v.setCode("VEH-001");
        v.setName("TATA Prima Tipper");
        v.setChassisNumber("CHS-12345");
        v.setEngineNumber("ENG-67890");
        v.setModel("Prima 2830.K");
        v.setBrand("TATA Motors");
        v.setStatus("ACTIVE");

        when(vehicleRepository.findByCompanyIdAndIsDeletedFalse(eq(COMPANY_ID), any()))
                .thenReturn(new PageImpl<>(List.of(v)));

        byte[] bytes = xlsxExportService.exportVehicles(COMPANY_ID);
        assertNotNull(bytes);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Vehicles");
            assertNotNull(sheet);
            Row dataRow = sheet.getRow(1);
            assertEquals("VEH-001", dataRow.getCell(0).getStringCellValue());
            assertEquals("TATA Prima Tipper", dataRow.getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Export Customers - Valid Workbook Structure")
    void testExportCustomers() throws Exception {
        Customer c = new Customer();
        c.setCode("CUST-001");
        c.setName("L&T Construction");
        c.setEmail("contact@lnt.com");
        c.setPhone("9876543210");
        c.setGstNumber("33AAAAA0000A1Z5");
        c.setCreditLimit(new BigDecimal("500000.00"));
        c.setStatus("ACTIVE");

        when(customerRepository.findByCompanyIdAndIsDeletedFalse(eq(COMPANY_ID), any()))
                .thenReturn(new PageImpl<>(List.of(c)));

        byte[] bytes = xlsxExportService.exportCustomers(COMPANY_ID);
        assertNotNull(bytes);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Customers");
            assertNotNull(sheet);
            Row dataRow = sheet.getRow(1);
            assertEquals("CUST-001", dataRow.getCell(0).getStringCellValue());
            assertEquals("L&T Construction", dataRow.getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Export Customer Ledger - Valid Workbook Structure")
    void testExportCustomerLedger() throws Exception {
        CustomerLedgerPrintDTO dto = new CustomerLedgerPrintDTO();
        dto.setCustomerCode("CUST-101");
        dto.setCustomerName("Infra Builders Ltd");
        dto.setOpeningBalance(BigDecimal.ZERO);
        dto.setClosingBalance(new BigDecimal("15000.00"));

        CustomerLedgerPrintDTO.CustomerLedgerItemDTO item = new CustomerLedgerPrintDTO.CustomerLedgerItemDTO();
        item.setTransactionDate(LocalDateTime.of(2026, 3, 1, 10, 0));
        item.setTransactionType("Sales Invoice");
        item.setReferenceNumber("INV-2026-001");
        item.setDebitAmount(new BigDecimal("15000.00"));
        item.setCreditAmount(BigDecimal.ZERO);
        item.setRunningBalance(new BigDecimal("15000.00"));

        dto.setItems(List.of(item));

        when(customerLedgerService.getLedgerPrintData(101L)).thenReturn(dto);

        byte[] bytes = xlsxExportService.exportCustomerLedger(101L);
        assertNotNull(bytes);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Customer Ledger");
            assertNotNull(sheet);
            Row dataRow = sheet.getRow(1);
            assertEquals("Sales Invoice", dataRow.getCell(1).getStringCellValue());
            assertEquals("INV-2026-001", dataRow.getCell(2).getStringCellValue());
            assertEquals(15000.0, dataRow.getCell(4).getNumericCellValue(), 0.01);
        }
    }

    @Test
    @DisplayName("Cross-Tenant Security Check - Denies Unauthorized Company Access")
    void testTenantSecurityDenial() {
        when(tenantAccess.resolveCompanyId(999L))
                .thenThrow(new AccessDeniedException("Access denied to company 999"));

        assertThrows(AccessDeniedException.class, () -> xlsxExportService.exportMaterials(999L));
    }
}
