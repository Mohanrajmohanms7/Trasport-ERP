package com.transport.erp.service;

import com.transport.erp.dto.StockIssueRequest;
import com.transport.erp.dto.StockMovementResponse;
import com.transport.erp.dto.StockReturnRequest;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Branch;
import com.transport.erp.model.InventoryTransaction;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.UomMaster;
import com.transport.erp.model.Warehouse;
import com.transport.erp.model.WarehouseStock;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.model.WorkOrderPart;
import com.transport.erp.repository.InventoryTransactionRepository;
import com.transport.erp.repository.WarehouseStockRepository;
import com.transport.erp.repository.WorkOrderPartRepository;
import com.transport.erp.repository.WorkOrderRepository;
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
class InventoryIssueReturnServiceTest {

    private static final Long COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;
    private static final Long BRANCH = 10L;
    private static final Long OTHER_BRANCH = 20L;

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderPartRepository workOrderPartRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private WarehouseService warehouseService;
    @Mock private InventoryService inventoryService;
    @Mock private TenantAccessService tenantAccess;
    @Mock private AuditService auditService;

    @Mock private InventoryValuationService valuationService;
    @InjectMocks private InventoryIssueService issueService;

    private AppUser companyAdmin;
    private Warehouse warehouse;
    private SparePart part;
    private WorkOrder order;
    private WorkOrderPart line;
    private final AtomicReference<BigDecimal> available = new AtomicReference<>(new BigDecimal("100.000"));

    @BeforeEach
    void setUp() {
        companyAdmin = user("admin", COMPANY, BRANCH, "COMPANY_ADMIN");
        stubTenant(companyAdmin, false);
        warehouse = warehouse(1L, COMPANY, BRANCH, "ACTIVE");
        part = part(5L, COMPANY);
        order = workOrder(20L, "IN_PROGRESS", COMPANY, BRANCH);
        line = workOrderPart(45L, order, part, new BigDecimal("10.000"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(tenantAccess.requireCurrentUser()).thenReturn(companyAdmin);
        when(workOrderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(order));
        when(workOrderPartRepository.findActiveLineForUpdate(45L, 20L)).thenReturn(Optional.of(line));
        when(warehouseService.requireUsable(1L, companyAdmin)).thenReturn(warehouse);
        when(workOrderPartRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            InventoryTransaction saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(40L);
            }
            return saved;
        });
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryService.decreaseAvailable(eq(warehouse), eq(part), any(), any())).thenAnswer(inv -> {
            BigDecimal qty = inv.getArgument(2);
            if (available.get().compareTo(qty) < 0) {
                throw new BusinessValidationException(
                        "Invalid Inventory",
                        "INVENTORY_INSUFFICIENT_STOCK",
                        "INVENTORY_INSUFFICIENT_STOCK: Insufficient stock available.",
                        "Check stock.");
            }
            available.set(available.get().subtract(qty));
            return stock(available.get());
        });
        when(inventoryService.increaseAvailable(eq(warehouse), eq(part), any(), any())).thenAnswer(inv -> {
            BigDecimal qty = inv.getArgument(2);
            available.set(available.get().add(qty));
            return stock(available.get());
        });
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_ISSUE))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_RETURN))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Valid issue decreases stock and records ISSUE")
    void validIssue() {
        StockMovementResponse response = issueService.issue(issue(1L, 20L, 45L, "5"), "admin");
        assertEquals(new BigDecimal("5.000"), response.getQuantity());
        assertEquals(new BigDecimal("5.000"), response.getIssuedQuantity());
        assertEquals(new BigDecimal("5.000"), response.getRemainingToIssue());
        assertEquals(new BigDecimal("95.000"), response.getAvailableQuantity());
        assertEquals("ISSUE", response.getTransactionType());
        assertEquals("IS-000040", response.getTransactionCode());
        assertEquals(new BigDecimal("15.00"), line.getUnitRate());
        assertEquals(new BigDecimal("150.00"), line.getLineTotal());
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals("ISSUE", tx.getValue().getTransactionType());
        assertEquals(COMPANY, tx.getValue().getCompanyId());
        assertEquals(BRANCH, tx.getValue().getBranchId());
        assertEquals(order, tx.getValue().getWorkOrder());
        assertEquals(line, tx.getValue().getWorkOrderPart());
        assertEquals(part, tx.getValue().getSparePart());
        verify(auditService).log(eq("admin"), eq("INVENTORY_ISSUED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
    }

    @Test
    @DisplayName("OPEN work order allows issue using existing parts lifecycle")
    void openWorkOrderAllowsIssue() {
        order.setStatus("OPEN");
        StockMovementResponse response = issueService.issue(issue(1L, 20L, 45L, "1"), "admin");
        assertEquals(new BigDecimal("1.000"), response.getIssuedQuantity());
    }

    @Test
    @DisplayName("Issue exact available stock")
    void issueExactAvailableStock() {
        available.set(new BigDecimal("5.000"));
        StockMovementResponse response = issueService.issue(issue(1L, 20L, 45L, "5"), "admin");
        assertEquals(new BigDecimal("0.000"), response.getAvailableQuantity());
    }

    @Test
    @DisplayName("Issue greater than stock is rejected")
    void issueGreaterThanStock() {
        available.set(new BigDecimal("4.000"));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "5"), "admin"));
        assertEquals("INVENTORY_INSUFFICIENT_STOCK", ex.getErrorCode());
        verify(transactionRepository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), eq("INVENTORY_ISSUED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Zero and negative issue quantity are rejected")
    void invalidIssueQuantity() {
        BusinessValidationException zero = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "0"), "admin"));
        assertEquals("INVENTORY_QUANTITY_INVALID", zero.getErrorCode());
        BusinessValidationException negative = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "-1"), "admin"));
        assertEquals("INVENTORY_QUANTITY_INVALID", negative.getErrorCode());
        verify(inventoryService, never()).decreaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Issue cannot exceed remaining work order quantity")
    void issueMoreThanRequested() {
        line.setIssuedQuantity(new BigDecimal("8.000"));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "3"), "admin"));
        assertEquals("INVENTORY_QUANTITY_INVALID", ex.getErrorCode());
        verify(inventoryService, never()).decreaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Multiple partial issues are allowed up to requested quantity")
    void multiplePartialIssues() {
        issueService.issue(issue(1L, 20L, 45L, "4"), "admin");
        issueService.issue(issue(1L, 20L, 45L, "3"), "admin");
        StockMovementResponse third = issueService.issue(issue(1L, 20L, 45L, "3"), "admin");
        assertEquals(new BigDecimal("10.000"), third.getIssuedQuantity());
        assertEquals(new BigDecimal("0.000"), third.getRemainingToIssue());
        BusinessValidationException extra = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "0.001"), "admin"));
        assertEquals("INVENTORY_QUANTITY_INVALID", extra.getErrorCode());
        verify(transactionRepository, times(3)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Completed and cancelled work orders reject issue")
    void issueRejectedForTerminalWorkOrders() {
        order.setStatus("COMPLETED");
        BusinessValidationException completed = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertEquals("INVENTORY_WORK_ORDER_INVALID_STATE", completed.getErrorCode());
        order.setStatus("CANCELLED");
        BusinessValidationException cancelled = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertEquals("INVENTORY_WORK_ORDER_INVALID_STATE", cancelled.getErrorCode());
        verify(inventoryService, never()).decreaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Inactive warehouse is rejected")
    void inactiveWarehouseRejected() {
        when(warehouseService.requireUsable(1L, companyAdmin)).thenThrow(new BusinessValidationException(
                "Invalid Warehouse", "WAREHOUSE_INACTIVE", "WAREHOUSE_INACTIVE: An active warehouse is required.", "Check status."));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertEquals("WAREHOUSE_INACTIVE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Cross-company warehouse is denied")
    void crossCompanyWarehouseDenied() {
        warehouse.setCompanyId(OTHER_COMPANY);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertTrue(ex.getMessage().contains("another company"));
        verify(inventoryService, never()).decreaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cross-branch warehouse is rejected")
    void crossBranchWarehouseRejected() {
        warehouse.setBranchId(OTHER_BRANCH);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertEquals("WAREHOUSE_BRANCH_INVALID", ex.getErrorCode());
    }

    @Test
    @DisplayName("Branch user cannot issue against another branch work order")
    void crossBranchWorkOrderDenied() {
        order.setBranchId(OTHER_BRANCH);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertTrue(ex.getMessage().contains("another branch"));
    }

    @Test
    @DisplayName("Cross-company work order is denied")
    void crossCompanyWorkOrderDenied() {
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(eq(OTHER_COMPANY));
        order.setCompanyId(OTHER_COMPANY);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertTrue(ex.getMessage().contains("another company"));
    }

    @Test
    @DisplayName("Driver cannot issue or return")
    void driverDenied() {
        AppUser driver = user("drv", COMPANY, BRANCH, "DRIVER");
        stubTenant(driver, false);
        when(tenantAccess.requireCurrentUser()).thenReturn(driver);
        doThrow(new AccessDeniedException("Access denied: warehouse updates require COMPANY_ADMIN or BRANCH_MANAGER."))
                .when(warehouseService).assertWriteAccess(driver);
        AccessDeniedException issue = assertThrows(AccessDeniedException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "drv"));
        AccessDeniedException ret = assertThrows(AccessDeniedException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "1"), "drv"));
        assertTrue(issue.getMessage().contains("COMPANY_ADMIN"));
        assertTrue(ret.getMessage().contains("COMPANY_ADMIN"));
        verify(inventoryService, never()).decreaseAvailable(any(), any(), any(), any());
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Missing and foreign work order parts are rejected")
    void invalidWorkOrderPart() {
        when(workOrderPartRepository.findActiveLineForUpdate(45L, 20L)).thenReturn(Optional.empty());
        BusinessValidationException missing = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "admin"));
        assertEquals("INVENTORY_WORK_ORDER_PART_REQUIRED", missing.getErrorCode());
        when(workOrderPartRepository.findActiveLineForUpdate(99L, 20L)).thenReturn(Optional.empty());
        BusinessValidationException other = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 99L, "1"), "admin"));
        assertEquals("INVENTORY_WORK_ORDER_PART_REQUIRED", other.getErrorCode());
    }

    @Test
    @DisplayName("Spare part is derived from the work order part")
    void sparePartDerivedFromLine() {
        StockMovementResponse response = issueService.issue(issue(1L, 20L, 45L, "1"), "admin");
        assertEquals(5L, response.getSparePartId());
        assertEquals("P5-FILTER", response.getSparePartCode());
        ArgumentCaptor<InventoryTransaction> tx = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).saveAndFlush(tx.capture());
        assertEquals(part, tx.getValue().getSparePart());
        assertEquals("admin", tx.getValue().getCreatedBy());
        assertEquals("ISSUE", tx.getValue().getTransactionType());
    }

    @Test
    @DisplayName("Valid return increases stock and records RETURN")
    void validReturn() {
        line.setIssuedQuantity(new BigDecimal("5.000"));
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_ISSUE))
                .thenReturn(new BigDecimal("5.000"));
        available.set(new BigDecimal("95.000"));
        StockMovementResponse response = issueService.returnStock(ret(1L, 20L, 45L, "2"), "admin");
        assertEquals(new BigDecimal("2.000"), response.getQuantity());
        assertEquals(new BigDecimal("2.000"), response.getReturnedQuantity());
        assertEquals(new BigDecimal("3.000"), response.getNetIssuedQuantity());
        assertEquals(new BigDecimal("97.000"), response.getAvailableQuantity());
        assertEquals("RT-000040", response.getTransactionCode());
        assertEquals(new BigDecimal("15.00"), line.getUnitRate());
        verify(auditService).log(eq("admin"), eq("INVENTORY_RETURNED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
    }

    @Test
    @DisplayName("Zero and negative return quantity are rejected")
    void invalidReturnQuantity() {
        line.setIssuedQuantity(new BigDecimal("5.000"));
        BusinessValidationException zero = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "0"), "admin"));
        assertEquals("INVENTORY_RETURN_QUANTITY_INVALID", zero.getErrorCode());
        BusinessValidationException negative = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "-2"), "admin"));
        assertEquals("INVENTORY_RETURN_QUANTITY_INVALID", negative.getErrorCode());
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Return cannot exceed issued quantity")
    void returnExceedsIssued() {
        line.setIssuedQuantity(new BigDecimal("10.000"));
        line.setReturnedQuantity(new BigDecimal("3.000"));
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_ISSUE))
                .thenReturn(new BigDecimal("10.000"));
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_RETURN))
                .thenReturn(new BigDecimal("3.000"));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "8"), "admin"));
        assertEquals("INVENTORY_RETURN_EXCEEDS_ISSUED", ex.getErrorCode());
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Multiple returns cannot exceed issued quantity")
    void multipleReturns() {
        line.setIssuedQuantity(new BigDecimal("10.000"));
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_ISSUE))
                .thenReturn(new BigDecimal("10.000"));
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_RETURN))
                .thenAnswer(inv -> line.getReturnedQuantity());
        issueService.returnStock(ret(1L, 20L, 45L, "2"), "admin");
        issueService.returnStock(ret(1L, 20L, 45L, "3"), "admin");
        assertEquals(new BigDecimal("5.000"), line.getReturnedQuantity());
        BusinessValidationException extra = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "6"), "admin"));
        assertEquals("INVENTORY_RETURN_EXCEEDS_ISSUED", extra.getErrorCode());
    }

    @Test
    @DisplayName("Return to a warehouse that did not issue is rejected")
    void returnDifferentWarehouseRejected() {
        line.setIssuedQuantity(new BigDecimal("5.000"));
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_ISSUE))
                .thenReturn(BigDecimal.ZERO);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "1"), "admin"));
        assertEquals("INVENTORY_RETURN_EXCEEDS_ISSUED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Completed and cancelled work orders reject return")
    void returnRejectedForTerminalWorkOrders() {
        line.setIssuedQuantity(new BigDecimal("5.000"));
        order.setStatus("COMPLETED");
        BusinessValidationException completed = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "1"), "admin"));
        assertEquals("INVENTORY_WORK_ORDER_INVALID_STATE", completed.getErrorCode());
        order.setStatus("CANCELLED");
        BusinessValidationException cancelled = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "1"), "admin"));
        assertEquals("INVENTORY_WORK_ORDER_INVALID_STATE", cancelled.getErrorCode());
        verify(inventoryService, never()).increaseAvailable(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Super Admin still cannot over-issue or over-return")
    void superAdminBusinessRulesApply() {
        AppUser root = user("root", null, null, "SUPER_ADMIN");
        stubTenant(root, true);
        when(tenantAccess.requireCurrentUser()).thenReturn(root);
        when(warehouseService.requireUsable(1L, root)).thenReturn(warehouse);
        line.setIssuedQuantity(new BigDecimal("10.000"));
        BusinessValidationException overIssue = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "1"), "root"));
        assertEquals("INVENTORY_QUANTITY_INVALID", overIssue.getErrorCode());
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_ISSUE))
                .thenReturn(new BigDecimal("10.000"));
        BusinessValidationException overReturn = assertThrows(BusinessValidationException.class,
                () -> issueService.returnStock(ret(1L, 20L, 45L, "11"), "root"));
        assertEquals("INVENTORY_RETURN_EXCEEDS_ISSUED", overReturn.getErrorCode());
    }

    @Test
    @DisplayName("Issue locks work order, part, warehouse, then stock")
    void issueLockOrder() {
        InOrder sequence = inOrder(warehouseService, workOrderRepository, workOrderPartRepository, inventoryService, transactionRepository, auditService);
        issueService.issue(issue(1L, 20L, 45L, "5"), "admin");
        sequence.verify(warehouseService).assertWriteAccess(companyAdmin);
        sequence.verify(workOrderRepository).findByIdForUpdate(20L);
        sequence.verify(workOrderPartRepository).findActiveLineForUpdate(45L, 20L);
        sequence.verify(warehouseService).requireUsable(1L, companyAdmin);
        sequence.verify(inventoryService).decreaseAvailable(eq(warehouse), eq(part), eq(new BigDecimal("5.000")), eq("admin"));
        sequence.verify(workOrderPartRepository).saveAndFlush(line);
        sequence.verify(transactionRepository).saveAndFlush(any());
        sequence.verify(auditService).log(eq("admin"), eq("INVENTORY_ISSUED"), eq("inventory_transactions"), eq(40L), nullable(String.class), any());
    }

    @Test
    @DisplayName("Failed issue after stock update does not record a transaction or audit")
    void issueRollbackWhenTransactionInsertFails() {
        doThrow(new RuntimeException("forced failure")).when(transactionRepository).saveAndFlush(any());
        assertThrows(RuntimeException.class, () -> issueService.issue(issue(1L, 20L, 45L, "5"), "admin"));
        verify(inventoryService).decreaseAvailable(eq(warehouse), eq(part), eq(new BigDecimal("5.000")), eq("admin"));
        verify(workOrderPartRepository).saveAndFlush(line);
        verify(auditService, never()).log(any(), eq("INVENTORY_ISSUED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Failed return after stock increase does not record a transaction or audit")
    void returnRollbackWhenTransactionInsertFails() {
        line.setIssuedQuantity(new BigDecimal("5.000"));
        when(transactionRepository.sumQuantity(1L, 45L, InventoryTransaction.TYPE_ISSUE))
                .thenReturn(new BigDecimal("5.000"));
        doThrow(new RuntimeException("forced failure")).when(transactionRepository).saveAndFlush(any());
        assertThrows(RuntimeException.class, () -> issueService.returnStock(ret(1L, 20L, 45L, "2"), "admin"));
        verify(inventoryService).increaseAvailable(eq(warehouse), eq(part), eq(new BigDecimal("2.000")), eq("admin"));
        verify(auditService, never()).log(any(), eq("INVENTORY_RETURNED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Concurrent second issue is rejected after stock is re-read")
    void concurrentIssueRejectedWhenInsufficient() {
        line.setQuantity(new BigDecimal("20.000"));
        available.set(new BigDecimal("10.000"));
        issueService.issue(issue(1L, 20L, 45L, "7"), "admin");
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> issueService.issue(issue(1L, 20L, 45L, "5"), "admin"));
        assertEquals("INVENTORY_INSUFFICIENT_STOCK", ex.getErrorCode());
        assertEquals(new BigDecimal("3.000"), available.get());
        verify(transactionRepository, times(1)).saveAndFlush(any());
    }

    @Test
    @DisplayName("Pessimistic locks protect stock and work order parts")
    void pessimisticLocksPresent() throws Exception {
        Lock stockLock = WarehouseStockRepository.class.getMethod("findActiveForUpdate", Long.class, Long.class)
                .getAnnotation(Lock.class);
        Lock partLock = WorkOrderPartRepository.class.getMethod("findActiveLineForUpdate", Long.class, Long.class)
                .getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, stockLock.value());
        assertEquals(LockModeType.PESSIMISTIC_WRITE, partLock.value());
    }

    @Test
    @DisplayName("Work order list and detail stay bounded")
    void workOrderQueriesStayBounded() throws Exception {
        String list = query(WorkOrderRepository.class, "searchIds");
        assertFalse(list.contains("InventoryTransaction"));
        assertFalse(list.contains("WarehouseStock"));
        String detail = WorkOrderPartRepository.class.getMethod("findActiveByWorkOrderId", Long.class)
                .getAnnotation(Query.class).value();
        assertFalse(detail.contains("InventoryTransaction"));
        String tx = InventoryTransactionRepository.class.getMethod("findDetailsByIds", java.util.Collection.class)
                .getAnnotation(Query.class).value();
        assertTrue(tx.contains("LEFT JOIN FETCH t.workOrder"));
        assertFalse(tx.contains("workOrderPart"));
        assertFalse(tx.contains("Journal"));
    }

    @Test
    @DisplayName("Issue service has no accounting coupling")
    void noAccountingCoupling() {
        for (Field field : InventoryIssueService.class.getDeclaredFields()) {
            String name = field.getType().getName();
            assertFalse(name.contains("Journal"));
            assertFalse(name.contains("Expense"));
            assertFalse(name.contains("VehicleMaintenance"));
            assertFalse(name.contains("FinancialYear"));
        }
    }

    private String query(Class<?> type, String method) throws Exception {
        for (var candidate : type.getMethods()) {
            if (candidate.getName().equals(method) && candidate.getAnnotation(Query.class) != null) {
                return candidate.getAnnotation(Query.class).value();
            }
        }
        return "";
    }

    private StockIssueRequest issue(Long warehouseId, Long workOrderId, Long partId, String quantity) {
        StockIssueRequest request = new StockIssueRequest();
        request.setWarehouseId(warehouseId);
        request.setWorkOrderId(workOrderId);
        request.setWorkOrderPartId(partId);
        request.setQuantity(new BigDecimal(quantity));
        return request;
    }

    private StockReturnRequest ret(Long warehouseId, Long workOrderId, Long partId, String quantity) {
        StockReturnRequest request = new StockReturnRequest();
        request.setWarehouseId(warehouseId);
        request.setWorkOrderId(workOrderId);
        request.setWorkOrderPartId(partId);
        request.setQuantity(new BigDecimal(quantity));
        return request;
    }

    private WarehouseStock stock(BigDecimal qty) {
        WarehouseStock row = new WarehouseStock();
        row.setId(30L);
        row.setWarehouse(warehouse);
        row.setSparePart(part);
        row.setAvailableQuantity(qty);
        return row;
    }

    private WorkOrder workOrder(Long id, String status, Long companyId, Long branchId) {
        WorkOrder row = new WorkOrder();
        row.setId(id);
        row.setWorkOrderNumber("WO-000020");
        row.setStatus(status);
        row.setCompanyId(companyId);
        row.setBranchId(branchId);
        row.setIsDeleted(false);
        return row;
    }

    private WorkOrderPart workOrderPart(
            Long id, WorkOrder order, SparePart sparePart, BigDecimal quantity, BigDecimal issued, BigDecimal returned) {
        WorkOrderPart row = new WorkOrderPart();
        row.setId(id);
        row.setWorkOrder(order);
        row.setSparePart(sparePart);
        row.setQuantity(quantity);
        row.setIssuedQuantity(issued);
        row.setReturnedQuantity(returned);
        row.setUnitRate(new BigDecimal("15.00"));
        row.setLineTotal(new BigDecimal("150.00"));
        row.setIsDeleted(false);
        return row;
    }

    private Warehouse warehouse(Long id, Long companyId, Long branchId, String status) {
        Warehouse row = new Warehouse();
        row.setId(id);
        row.setCode("MAIN");
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
