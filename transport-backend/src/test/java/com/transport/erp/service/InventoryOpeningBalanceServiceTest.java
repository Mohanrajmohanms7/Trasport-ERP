package com.transport.erp.service;

import com.transport.erp.dto.OpeningBalanceRequest;
import com.transport.erp.dto.WarehouseStockResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Branch;
import com.transport.erp.model.InventoryTransaction;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.UomMaster;
import com.transport.erp.model.Warehouse;
import com.transport.erp.model.WarehouseStock;
import com.transport.erp.repository.InventoryTransactionRepository;
import com.transport.erp.repository.SparePartRepository;
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class InventoryOpeningBalanceServiceTest {

    private static final Long COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;
    private static final Long BRANCH = 10L;
    private static final Long OTHER_BRANCH = 20L;

    @Mock private WarehouseStockRepository stockRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private SparePartRepository sparePartRepository;
    @Mock private WarehouseService warehouseService;
    @Mock private TenantAccessService tenantAccess;
    @Mock private AuditService auditService;

    @InjectMocks private InventoryService inventoryService;

    private AppUser companyAdmin;
    private Warehouse warehouse;
    private SparePart part;

    @BeforeEach
    void setUp() {
        companyAdmin = user("admin", COMPANY, BRANCH, "COMPANY_ADMIN");
        stubTenant(companyAdmin, false);
        warehouse = warehouse(1L, COMPANY, BRANCH, "ACTIVE", false);
        part = part(5L, COMPANY, false);
        when(tenantAccess.requireCurrentUser()).thenReturn(companyAdmin);
        when(warehouseService.requireUsable(1L, companyAdmin)).thenReturn(warehouse);
        when(sparePartRepository.findDetailById(5L)).thenReturn(Optional.of(part));
        when(stockRepository.findActiveForUpdate(1L, 5L)).thenReturn(Optional.empty());
        when(stockRepository.saveAndFlush(any())).thenAnswer(inv -> {
            WarehouseStock saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(30L);
            }
            return saved;
        });
        when(transactionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            InventoryTransaction saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(40L);
            }
            return saved;
        });
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findDetailById(30L)).thenAnswer(inv -> Optional.of(stockWithMasters(new BigDecimal("100.000"))));
    }

    @Test
    @DisplayName("Authorized admin creates opening stock")
    void authorizedOpeningBalance() {
        WarehouseStockResponse response = inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin");
        assertEquals(new BigDecimal("100.000"), response.getAvailableQuantity());
        assertEquals("P5-FILTER", response.getSparePartCode());
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals("OPENING_BALANCE", tx.getValue().getTransactionType());
        assertEquals(new BigDecimal("100.000"), tx.getValue().getQuantity());
        assertEquals(COMPANY, tx.getValue().getCompanyId());
        assertEquals(BRANCH, tx.getValue().getBranchId());
        assertEquals("admin", tx.getValue().getCreatedBy());
        assertEquals(warehouse, tx.getValue().getWarehouse());
        assertEquals(part, tx.getValue().getSparePart());
        verify(auditService).log(eq("admin"), eq("OPENING_BALANCE_CREATED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("Branch manager can post opening stock in own branch")
    void branchManagerOpeningBalance() {
        AppUser manager = user("mgr", COMPANY, BRANCH, "BRANCH_MANAGER");
        stubTenant(manager, false);
        when(warehouseService.requireUsable(1L, manager)).thenReturn(warehouse);
        WarehouseStockResponse response = inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "mgr");
        assertEquals(new BigDecimal("100.000"), response.getAvailableQuantity());
        verify(auditService).log(eq("mgr"), eq("OPENING_BALANCE_CREATED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
    }

    @Test
    @DisplayName("Super Admin can post opening stock")
    void superAdminOpeningBalance() {
        AppUser superAdmin = user("root", COMPANY, null, "SUPER_ADMIN");
        stubTenant(superAdmin, true);
        when(warehouseService.requireUsable(1L, superAdmin)).thenReturn(warehouse);
        WarehouseStockResponse response = inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "root");
        assertEquals(COMPANY, response.getCompanyId());
        verify(auditService).log(eq("root"), eq("OPENING_BALANCE_CREATED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
    }

    @Test
    @DisplayName("Existing balance is increased by a second opening transaction")
    void existingBalanceAddsQuantity() {
        WarehouseStock existing = stockWithMasters(new BigDecimal("50.000"));
        existing.setId(30L);
        when(stockRepository.findActiveForUpdate(1L, 5L)).thenReturn(Optional.of(existing));
        when(stockRepository.findDetailById(30L)).thenAnswer(inv -> Optional.of(existing));
        WarehouseStockResponse response = inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("20")), "admin");
        assertEquals(new BigDecimal("70.000"), response.getAvailableQuantity());
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals(new BigDecimal("20.000"), tx.getValue().getQuantity());
    }

    @Test
    @DisplayName("Driver cannot create opening stock")
    void driverDeniedOpening() {
        AppUser driver = user("drv", COMPANY, BRANCH, "DRIVER");
        stubTenant(driver, false);
        doThrow(new AccessDeniedException("Access denied: warehouse updates require COMPANY_ADMIN or BRANCH_MANAGER."))
                .when(warehouseService).assertWriteAccess(driver);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "drv"));
        assertTrue(ex.getMessage().contains("COMPANY_ADMIN"));
        verify(stockRepository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), eq("OPENING_BALANCE_CREATED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cross-company warehouse is rejected")
    void crossCompanyWarehouseRejected() {
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(warehouseService).requireUsable(eq(1L), any());
        assertThrows(AccessDeniedException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin"));
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Cross-company spare part is rejected")
    void crossCompanySparePartRejected() {
        part.setCompanyId(OTHER_COMPANY);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin"));
        assertEquals("SPARE_PART_NOT_FOUND", ex.getErrorCode());
        verify(auditService, never()).log(any(), eq("OPENING_BALANCE_CREATED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cross-branch warehouse is rejected")
    void crossBranchWarehouseRejected() {
        doThrow(new AccessDeniedException("Access denied: Warehouse belongs to another branch."))
                .when(warehouseService).requireUsable(eq(1L), any());
        assertThrows(AccessDeniedException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin"));
    }

    @Test
    @DisplayName("Deleted warehouse is rejected")
    void deletedWarehouseRejected() {
        when(warehouseService.requireUsable(1L, companyAdmin))
                .thenThrow(new BusinessValidationException("Invalid Warehouse", "WAREHOUSE_NOT_FOUND", "WAREHOUSE_NOT_FOUND: missing", "x"));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin"));
        assertEquals("WAREHOUSE_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Inactive warehouse is rejected")
    void inactiveWarehouseRejected() {
        when(warehouseService.requireUsable(1L, companyAdmin))
                .thenThrow(new BusinessValidationException("Invalid Warehouse", "WAREHOUSE_INACTIVE", "WAREHOUSE_INACTIVE: inactive", "x"));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin"));
        assertEquals("WAREHOUSE_INACTIVE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Deleted spare part is rejected")
    void deletedSparePartRejected() {
        part.setIsDeleted(true);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin"));
        assertEquals("SPARE_PART_DELETED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Negative opening quantity is rejected")
    void negativeQuantityRejected() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("-10")), "admin"));
        assertEquals("OPENING_BALANCE_QUANTITY_INVALID", ex.getErrorCode());
        verify(stockRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Zero opening quantity is rejected")
    void zeroQuantityRejected() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, BigDecimal.ZERO), "admin"));
        assertEquals("OPENING_BALANCE_QUANTITY_INVALID", ex.getErrorCode());
    }

    @Test
    @DisplayName("Client cannot set company, branch, type, or createdBy")
    void clientTamperingIgnored() {
        OpeningBalanceRequest request = opening(1L, 5L, new BigDecimal("100"));
        inventoryService.createOpeningBalance(request, "admin");
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals(COMPANY, tx.getValue().getCompanyId());
        assertEquals(BRANCH, tx.getValue().getBranchId());
        assertEquals("OPENING_BALANCE", tx.getValue().getTransactionType());
        assertEquals("admin", tx.getValue().getCreatedBy());
    }

    @Test
    @DisplayName("Concurrent first inserts collapse to one balance")
    void concurrentFirstInsertUsesLockRetry() {
        WarehouseStock winner = stockWithMasters(new BigDecimal("100.000"));
        winner.setId(30L);
        when(stockRepository.findActiveForUpdate(1L, 5L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException(
                "dup", new SQLException("duplicate key uk_warehouse_stock_warehouse_part")))
                .doAnswer(inv -> inv.getArgument(0))
                .when(stockRepository).saveAndFlush(any());
        when(stockRepository.findDetailById(30L)).thenAnswer(inv -> Optional.of(winner));
        WarehouseStockResponse response = inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("50")), "admin");
        assertEquals(new BigDecimal("150.000"), response.getAvailableQuantity());
        verify(stockRepository, times(2)).findActiveForUpdate(1L, 5L);
        verify(transactionRepository, times(1)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Opening balance locks then writes transaction")
    void openingBalanceQueryOrder() {
        InOrder order = inOrder(warehouseService, sparePartRepository, stockRepository, transactionRepository, auditService);
        inventoryService.createOpeningBalance(opening(1L, 5L, new BigDecimal("100")), "admin");
        order.verify(warehouseService).assertWriteAccess(companyAdmin);
        order.verify(warehouseService).requireUsable(1L, companyAdmin);
        order.verify(sparePartRepository).findDetailById(5L);
        order.verify(stockRepository).findActiveForUpdate(1L, 5L);
        order.verify(stockRepository).saveAndFlush(any());
        order.verify(transactionRepository).saveAndFlush(any());
        order.verify(auditService).log(eq("admin"), eq("OPENING_BALANCE_CREATED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
    }

    @Test
    @DisplayName("Rejected opening is not audited as success")
    void rejectedOpeningNotAudited() {
        assertThrows(BusinessValidationException.class,
                () -> inventoryService.createOpeningBalance(opening(1L, 5L, BigDecimal.ZERO), "admin"));
        verify(auditService, never()).log(any(), eq("OPENING_BALANCE_CREATED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Stock and transaction lists use bounded fetch joins")
    void listQueriesAreBounded() throws Exception {
        String stock = WarehouseStockRepository.class.getMethod("findDetailsByIds", java.util.Collection.class)
                .getAnnotation(Query.class).value();
        assertTrue(stock.contains("JOIN FETCH s.warehouse"));
        assertTrue(stock.contains("JOIN FETCH s.sparePart"));
        assertFalse(stock.contains("WorkOrder"));
        String tx = InventoryTransactionRepository.class.getMethod("findDetailsByIds", java.util.Collection.class)
                .getAnnotation(Query.class).value();
        assertTrue(tx.contains("JOIN FETCH t.warehouse"));
        assertTrue(tx.contains("JOIN FETCH t.sparePart"));
    }

    @Test
    @DisplayName("Inventory service has no accounting coupling")
    void noAccountingCoupling() {
        for (Field field : InventoryService.class.getDeclaredFields()) {
            String name = field.getType().getName();
            assertFalse(name.contains("Journal"));
            assertFalse(name.contains("Expense"));
            assertFalse(name.contains("WorkOrder"));
            assertFalse(name.contains("VehicleMaintenance"));
        }
    }

    @Test
    @DisplayName("Decrease available uses the stock lock and rejects insufficient quantity")
    void decreaseAvailableLocksAndRejectsInsufficient() {
        WarehouseStock existing = stockWithMasters(new BigDecimal("10.000"));
        when(stockRepository.findActiveForUpdate(1L, 5L)).thenReturn(Optional.of(existing));
        WarehouseStock remaining = inventoryService.decreaseAvailable(warehouse, part, new BigDecimal("7.000"), "admin");
        assertEquals(new BigDecimal("3.000"), remaining.getAvailableQuantity());
        verify(stockRepository).findActiveForUpdate(1L, 5L);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> inventoryService.decreaseAvailable(warehouse, part, new BigDecimal("4.000"), "admin"));
        assertEquals("INVENTORY_INSUFFICIENT_STOCK", ex.getErrorCode());
    }

    @Test
    @DisplayName("Receipt increase then issue decrease nets without overwrite")
    void receiptThenIssueNets() {
        WarehouseStock existing = stockWithMasters(new BigDecimal("100.000"));
        when(stockRepository.findActiveForUpdate(1L, 5L)).thenReturn(Optional.of(existing));
        inventoryService.increaseAvailable(warehouse, part, new BigDecimal("20.000"), "admin");
        assertEquals(new BigDecimal("120.000"), existing.getAvailableQuantity());
        inventoryService.decreaseAvailable(warehouse, part, new BigDecimal("5.000"), "admin");
        assertEquals(new BigDecimal("115.000"), existing.getAvailableQuantity());
    }

    @Test
    @DisplayName("Signed transaction sum query matches stock invariant")
    void signedQuantityQueryMatchesStockInvariant() throws Exception {
        String signed = InventoryTransactionRepository.class
                .getMethod("sumSignedQuantity", Long.class, Long.class)
                .getAnnotation(Query.class).value();
        assertTrue(signed.contains("OPENING") || signed.contains("ELSE t.quantity"));
        assertTrue(signed.contains("ISSUE"));
        assertTrue(signed.contains("-t.quantity"));
    }

    private OpeningBalanceRequest opening(Long warehouseId, Long sparePartId, BigDecimal quantity) {
        OpeningBalanceRequest request = new OpeningBalanceRequest();
        request.setWarehouseId(warehouseId);
        request.setSparePartId(sparePartId);
        request.setQuantity(quantity);
        request.setDescription("Initial workshop stock");
        return request;
    }

    private WarehouseStock stockWithMasters(BigDecimal qty) {
        WarehouseStock stock = new WarehouseStock();
        stock.setId(30L);
        stock.setCompanyId(COMPANY);
        stock.setBranchId(BRANCH);
        stock.setWarehouse(warehouse);
        stock.setSparePart(part);
        stock.setAvailableQuantity(qty);
        stock.setIsDeleted(false);
        return stock;
    }

    private Warehouse warehouse(Long id, Long companyId, Long branchId, String status, boolean deleted) {
        Warehouse row = new Warehouse();
        row.setId(id);
        row.setCode("MAIN");
        row.setName("Main Workshop Store");
        row.setCompanyId(companyId);
        row.setBranchId(branchId);
        row.setStatus(status);
        row.setIsDeleted(deleted);
        Branch branch = new Branch();
        branch.setId(branchId);
        branch.setCode("BR-1");
        branch.setName("Branch 1");
        row.setBranch(branch);
        return row;
    }

    private SparePart part(Long id, Long companyId, boolean deleted) {
        SparePart row = new SparePart();
        row.setId(id);
        row.setCompanyId(companyId);
        row.setCode("P5-FILTER");
        row.setName("Oil filter");
        row.setStatus("ACTIVE");
        row.setIsDeleted(deleted);
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
