package com.transport.erp.service;

import com.transport.erp.dto.StockReceiptRequest;
import com.transport.erp.dto.StockReceiptResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Branch;
import com.transport.erp.model.InventoryTransaction;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.Supplier;
import com.transport.erp.model.UomMaster;
import com.transport.erp.model.Warehouse;
import com.transport.erp.model.WarehouseStock;
import com.transport.erp.repository.InventoryTransactionRepository;
import com.transport.erp.repository.SparePartRepository;
import com.transport.erp.repository.SupplierRepository;
import com.transport.erp.repository.WarehouseStockRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.LockModeType;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryReceiptServiceTest {

    private static final Long COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;
    private static final Long BRANCH = 10L;
    private static final Long OTHER_BRANCH = 20L;

    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private SparePartRepository sparePartRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private WarehouseService warehouseService;
    @Mock private InventoryService inventoryService;
    @Mock private TenantAccessService tenantAccess;
    @Mock private AuditService auditService;

    @Mock private InventoryValuationService valuationService;
    @InjectMocks private InventoryReceiptService receiptService;

    private AppUser companyAdmin;
    private Warehouse warehouse;
    private SparePart part;
    private final AtomicReference<BigDecimal> available = new AtomicReference<>(new BigDecimal("100.000"));

    @BeforeEach
    void setUp() {
        companyAdmin = user("admin", COMPANY, BRANCH, "COMPANY_ADMIN");
        stubTenant(companyAdmin, false);
        warehouse = warehouse(1L, COMPANY, BRANCH, "ACTIVE");
        part = part(5L, COMPANY);
        when(tenantAccess.requireCurrentUser()).thenReturn(companyAdmin);
        when(warehouseService.requireActive(1L, companyAdmin)).thenReturn(warehouse);
        when(sparePartRepository.findDetailById(5L)).thenReturn(Optional.of(part));
        when(transactionRepository.countReceiptExternalReference(any(), any())).thenReturn(0L);
        when(transactionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            InventoryTransaction saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(40L);
            }
            return saved;
        });
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryService.increaseAvailable(eq(warehouse), any(), any(), any())).thenAnswer(inv -> {
            BigDecimal qty = inv.getArgument(2);
            available.set(available.get().add(qty));
            return stock(available.get());
        });
    }

    @Test
    @DisplayName("Valid receipt increases stock and records RECEIPT with RC- reference")
    void validReceipt() {
        StockReceiptResponse response = receiptService.receive(receipt(1L, 5L, "20"), "admin");
        assertEquals(new BigDecimal("20.000"), response.getQuantity());
        assertEquals(new BigDecimal("120.000"), response.getAvailableQuantity());
        assertEquals("RECEIPT", response.getTransactionType());
        assertEquals("RC-000040", response.getTransactionCode());
        assertEquals("P5-FILTER", response.getSparePartCode());
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals("RECEIPT", tx.getValue().getTransactionType());
        assertEquals(COMPANY, tx.getValue().getCompanyId());
        assertEquals(BRANCH, tx.getValue().getBranchId());
        assertNull(tx.getValue().getWorkOrder());
        assertNull(tx.getValue().getWorkOrderPart());
        assertEquals("admin", tx.getValue().getCreatedBy());
        verify(auditService).log(eq("admin"), eq("INVENTORY_RECEIPT_CREATED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
        verify(warehouseService).requireActive(1L, companyAdmin);
        verify(warehouseService, never()).requireUsable(any(), any());
    }

    @Test
    @DisplayName("Two sequential receipts add 10 and 20 onto 100")
    void concurrentReceiptsSerializeToOneHundredThirty() {
        receiptService.receive(receipt(1L, 5L, "10"), "admin");
        StockReceiptResponse second = receiptService.receive(receipt(1L, 5L, "20"), "admin");
        assertEquals(new BigDecimal("130.000"), second.getAvailableQuantity());
        verify(inventoryService, times(2)).increaseAvailable(eq(warehouse), eq(part), any(), eq("admin"));
    }

    @Test
    @DisplayName("Receipt then issue nets to the serialized quantity")
    void concurrentReceiptAndIssue() {
        receiptService.receive(receipt(1L, 5L, "20"), "admin");
        available.set(available.get().subtract(new BigDecimal("5.000")));
        assertEquals(new BigDecimal("115.000"), available.get());
    }

    @Test
    @DisplayName("Receipts for different spare parts do not lock the warehouse")
    void concurrentDifferentPartsDoNotLockWarehouse() {
        SparePart other = part(6L, COMPANY);
        other.setCode("P6-BELT");
        when(sparePartRepository.findDetailById(6L)).thenReturn(Optional.of(other));
        receiptService.receive(receipt(1L, 5L, "10"), "admin");
        receiptService.receive(receipt(1L, 6L, "8"), "admin");
        verify(warehouseService, never()).requireUsable(any(), any());
        verify(warehouseService, times(2)).requireActive(1L, companyAdmin);
        verify(inventoryService).increaseAvailable(warehouse, part, new BigDecimal("10.000"), "admin");
        verify(inventoryService).increaseAvailable(warehouse, other, new BigDecimal("8.000"), "admin");
    }

    @Test
    @DisplayName("Zero and negative receipt quantities are rejected")
    void invalidQuantityRejected() {
        assertEquals("INVENTORY_RECEIPT_QUANTITY_INVALID",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(receipt(1L, 5L, "0"), "admin")).getErrorCode());
        assertEquals("INVENTORY_RECEIPT_QUANTITY_INVALID",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(receipt(1L, 5L, "-1"), "admin")).getErrorCode());
        assertEquals("INVENTORY_RECEIPT_QUANTITY_INVALID",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(receipt(1L, 5L, null), "admin")).getErrorCode());
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
        verify(auditService, never()).log(any(), eq("INVENTORY_RECEIPT_CREATED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Missing warehouse or spare part is rejected")
    void requiredFields() {
        assertEquals("INVENTORY_RECEIPT_WAREHOUSE_REQUIRED",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(receipt(null, 5L, "1"), "admin")).getErrorCode());
        assertEquals("INVENTORY_RECEIPT_SPARE_PART_REQUIRED",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(receipt(1L, null, "1"), "admin")).getErrorCode());
    }

    @Test
    @DisplayName("Company 1 cannot receipt into Company 2 warehouse")
    void companyIsolation() {
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(warehouseService).requireActive(9L, companyAdmin);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> receiptService.receive(receipt(9L, 5L, "5"), "admin"));
        assertTrue(ex.getMessage().contains("company"));
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Branch 1 cannot receipt into Branch 2 warehouse")
    void branchIsolation() {
        doThrow(new AccessDeniedException("Access denied: Warehouse belongs to another branch."))
                .when(warehouseService).requireActive(8L, companyAdmin);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> receiptService.receive(receipt(8L, 5L, "5"), "admin"));
        assertTrue(ex.getMessage().contains("branch"));
    }

    @Test
    @DisplayName("DRIVER cannot create a receipt")
    void driverDenied() {
        AppUser driver = user("driver1", COMPANY, BRANCH, "DRIVER");
        when(tenantAccess.requireCurrentUser()).thenReturn(driver);
        doThrow(new AccessDeniedException("Access denied: warehouse updates require COMPANY_ADMIN or BRANCH_MANAGER."))
                .when(warehouseService).assertWriteAccess(driver);
        assertThrows(AccessDeniedException.class, () -> receiptService.receive(receipt(1L, 5L, "5"), "driver1"));
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Super Admin can receipt using existing bypass")
    void superAdminBypass() {
        AppUser root = user("root", null, null, "SUPER_ADMIN");
        stubTenant(root, true);
        when(tenantAccess.requireCurrentUser()).thenReturn(root);
        when(warehouseService.requireActive(1L, root)).thenReturn(warehouse);
        StockReceiptResponse response = receiptService.receive(receipt(1L, 5L, "3"), "root");
        assertEquals(new BigDecimal("103.000"), response.getAvailableQuantity());
        verify(warehouseService).assertWriteAccess(root);
    }

    @Test
    @DisplayName("Client cannot set company, branch, type, createdBy, or generated reference")
    void clientTamperingIgnored() {
        StockReceiptRequest request = receipt(1L, 5L, "5");
        receiptService.receive(request, "admin");
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals(COMPANY, tx.getValue().getCompanyId());
        assertEquals(BRANCH, tx.getValue().getBranchId());
        assertEquals("RECEIPT", tx.getValue().getTransactionType());
        assertEquals("admin", tx.getValue().getCreatedBy());
        assertEquals("RC-000040", tx.getValue().getCode());
    }

    @Test
    @DisplayName("Opening balance type is not used for later stock increase")
    void doesNotWriteOpeningBalance() {
        receiptService.receive(receipt(1L, 5L, "20"), "admin");
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals("RECEIPT", tx.getValue().getTransactionType());
        assertFalse("OPENING_BALANCE".equals(tx.getValue().getTransactionType()));
    }

    @Test
    @DisplayName("Optional supplier and unit rate are stored without valuation")
    void optionalSupplierAndRate() {
        Supplier supplier = new Supplier();
        supplier.setId(4L);
        supplier.setCompanyId(COMPANY);
        supplier.setCode("SUP-1");
        supplier.setName("Filter Traders");
        supplier.setIsDeleted(false);
        when(supplierRepository.findById(4L)).thenReturn(Optional.of(supplier));
        StockReceiptRequest request = receipt(1L, 5L, "20");
        request.setSupplierId(4L);
        request.setUnitRate(new BigDecimal("125.00"));
        request.setReferenceNumber("SUP-REC-001");
        StockReceiptResponse response = receiptService.receive(request, "admin");
        assertEquals(4L, response.getSupplierId());
        assertEquals(new BigDecimal("125.00"), response.getUnitRate());
        assertEquals("SUP-REC-001", response.getExternalReference());
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals(supplier, tx.getValue().getSupplier());
        assertEquals(new BigDecimal("125.00"), tx.getValue().getUnitRate());
    }

    @Test
    @DisplayName("Duplicate external reference is rejected")
    void duplicateExternalReference() {
        when(transactionRepository.countReceiptExternalReference(COMPANY, "SUP-REC-001")).thenReturn(1L);
        StockReceiptRequest request = receipt(1L, 5L, "20");
        request.setReferenceNumber("SUP-REC-001");
        assertEquals("INVENTORY_RECEIPT_REFERENCE_DUPLICATE",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(request, "admin")).getErrorCode());
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Concurrent duplicate reference maps unique index to validation")
    void concurrentDuplicateReference() {
        doThrow(new DataIntegrityViolationException(
                "dup", new SQLException("duplicate key uk_inventory_tx_company_receipt_ext_ref")))
                .when(transactionRepository).saveAndFlush(any());
        StockReceiptRequest request = receipt(1L, 5L, "20");
        request.setReferenceNumber("SUP-REC-001");
        assertEquals("INVENTORY_RECEIPT_REFERENCE_DUPLICATE",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(request, "admin")).getErrorCode());
    }

    @Test
    @DisplayName("Wrong-company supplier is rejected")
    void supplierCompanyIsolation() {
        Supplier supplier = new Supplier();
        supplier.setId(9L);
        supplier.setCompanyId(OTHER_COMPANY);
        supplier.setIsDeleted(false);
        when(supplierRepository.findById(9L)).thenReturn(Optional.of(supplier));
        StockReceiptRequest request = receipt(1L, 5L, "20");
        request.setSupplierId(9L);
        assertEquals("INVENTORY_RECEIPT_SUPPLIER_INVALID",
                assertThrows(BusinessValidationException.class, () -> receiptService.receive(request, "admin")).getErrorCode());
    }

    @Test
    @DisplayName("Failed transaction insert after stock increase is not audited")
    void rollbackWhenTransactionInsertFails() {
        doThrow(new RuntimeException("forced failure")).when(transactionRepository).saveAndFlush(any());
        assertThrows(RuntimeException.class, () -> receiptService.receive(receipt(1L, 5L, "20"), "admin"));
        verify(inventoryService).increaseAvailable(eq(warehouse), eq(part), eq(new BigDecimal("20.000")), eq("admin"));
        verify(auditService, never()).log(any(), eq("INVENTORY_RECEIPT_CREATED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Receipt uses stock-row lock and signed quantity reconciliation query")
    void lockAndReconciliationQueries() throws Exception {
        Lock stockLock = WarehouseStockRepository.class.getMethod("findActiveForUpdate", Long.class, Long.class)
                .getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, stockLock.value());
        String signed = InventoryTransactionRepository.class
                .getMethod("sumSignedQuantity", Long.class, Long.class)
                .getAnnotation(Query.class).value();
        assertTrue(signed.contains("ISSUE"));
        assertTrue(signed.contains("-t.quantity"));
        String list = InventoryTransactionRepository.class.getMethod("findDetailsByIds", java.util.Collection.class)
                .getAnnotation(Query.class).value();
        assertTrue(list.contains("JOIN FETCH t.warehouse"));
        assertTrue(list.contains("JOIN FETCH t.sparePart"));
        assertTrue(list.contains("LEFT JOIN FETCH t.supplier"));
    }

    @Test
    @DisplayName("Receipt service has no accounting coupling")
    void noAccountingCoupling() {
        for (Field field : InventoryReceiptService.class.getDeclaredFields()) {
            String name = field.getType().getName();
            assertFalse(name.contains("Journal"));
            assertFalse(name.contains("Expense"));
            assertFalse(name.contains("Payment"));
            assertFalse(name.contains("WorkOrder"));
            assertFalse(name.contains("VehicleMaintenance"));
        }
    }

    @Test
    @DisplayName("Receipt lock then write order")
    void receiptQueryOrder() {
        InOrder order = inOrder(warehouseService, sparePartRepository, inventoryService, transactionRepository, auditService);
        receiptService.receive(receipt(1L, 5L, "20"), "admin");
        order.verify(warehouseService).assertWriteAccess(companyAdmin);
        order.verify(warehouseService).requireActive(1L, companyAdmin);
        order.verify(sparePartRepository).findDetailById(5L);
        order.verify(inventoryService).increaseAvailable(warehouse, part, new BigDecimal("20.000"), "admin");
        order.verify(transactionRepository).saveAndFlush(any());
        order.verify(auditService).log(eq("admin"), eq("INVENTORY_RECEIPT_CREATED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
    }

    private StockReceiptRequest receipt(Long warehouseId, Long sparePartId, String quantity) {
        StockReceiptRequest request = new StockReceiptRequest();
        request.setWarehouseId(warehouseId);
        request.setSparePartId(sparePartId);
        request.setQuantity(quantity == null ? null : new BigDecimal(quantity));
        return request;
    }

    private WarehouseStock stock(BigDecimal qty) {
        WarehouseStock row = new WarehouseStock();
        row.setId(30L);
        row.setAvailableQuantity(qty);
        row.setWarehouse(warehouse);
        row.setSparePart(part);
        return row;
    }

    private Warehouse warehouse(Long id, Long companyId, Long branchId, String status) {
        Warehouse row = new Warehouse();
        row.setId(id);
        row.setCode("MAIN-WS");
        row.setName("Main Workshop Store");
        row.setCompanyId(companyId);
        row.setBranchId(branchId);
        row.setStatus(status);
        row.setIsDeleted(false);
        Branch branch = new Branch();
        branch.setId(branchId);
        branch.setCode("BR-1");
        branch.setName("Branch 1");
        row.setBranch(branch);
        return row;
    }

    private SparePart part(Long id, Long companyId) {
        SparePart row = new SparePart();
        row.setId(id);
        row.setCompanyId(companyId);
        row.setCode("P5-FILTER");
        row.setName("Oil filter");
        row.setStatus("ACTIVE");
        row.setIsDeleted(false);
        UomMaster uom = new UomMaster();
        uom.setId(7L);
        uom.setCode("PCS");
        uom.setName("Piece");
        row.setDefaultUom(uom);
        return row;
    }

    private void stubTenant(AppUser user, boolean superAdmin) {
        when(tenantAccess.requireCurrentUser()).thenReturn(user);
        when(tenantAccess.isSuperAdmin(user)).thenReturn(superAdmin);
        when(tenantAccess.resolveCompanyId(any())).thenReturn(user.getCompanyId());
        org.mockito.Mockito.doAnswer(inv -> {
            Long companyId = inv.getArgument(0);
            if (!superAdmin && (companyId == null || !companyId.equals(user.getCompanyId()))) {
                throw new AccessDeniedException("Access denied to another company's data");
            }
            return null;
        }).when(tenantAccess).assertCompanyAccess(nullable(Long.class));
    }

    private AppUser user(String username, Long companyId, Long branchId, String roleCode) {
        AppUser row = new AppUser();
        row.setId(1L);
        row.setUsername(username);
        row.setName(username);
        row.setCompanyId(companyId);
        row.setBranchId(branchId);
        row.setIsDeleted(false);
        AppRole role = new AppRole();
        role.setCode(roleCode);
        row.setRoles(new HashSet<>(Set.of(role)));
        return row;
    }
}
