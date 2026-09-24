package com.transport.erp.service;

import com.transport.erp.dto.WarehouseRequest;
import com.transport.erp.dto.WarehouseResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Branch;
import com.transport.erp.model.Warehouse;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.WarehouseRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarehouseServiceTest {

    private static final Long COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;
    private static final Long BRANCH = 10L;
    private static final Long OTHER_BRANCH = 20L;

    @Mock private WarehouseRepository warehouseRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private TenantAccessService tenantAccess;
    @Mock private AuditService auditService;

    @InjectMocks private WarehouseService warehouseService;

    private AppUser companyAdmin;
    private Branch branch;

    @BeforeEach
    void setUp() {
        companyAdmin = user("admin", COMPANY, BRANCH, "COMPANY_ADMIN");
        stubTenant(companyAdmin, false);
        branch = branch(BRANCH, COMPANY);
        when(branchRepository.findById(BRANCH)).thenReturn(Optional.of(branch));
        when(warehouseRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Warehouse saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(8L);
            }
            return saved;
        });
        when(warehouseRepository.findDetailById(8L)).thenAnswer(inv -> Optional.of(storedWarehouse()));
        when(warehouseRepository.findByIdForUpdate(8L)).thenAnswer(inv -> Optional.of(storedWarehouse()));
    }

    @Test
    @DisplayName("Company admin creates warehouse in own company")
    void companyAdminCreatesOwnWarehouse() {
        WarehouseRequest request = request("WS-1", "Main Workshop Store", OTHER_COMPANY, BRANCH);
        WarehouseResponse response = warehouseService.create(request, "admin");
        assertEquals("WS-1", response.getCode());
        assertEquals(COMPANY, response.getCompanyId());
        assertEquals(BRANCH, response.getBranchId());
        verify(auditService).log(eq("admin"), eq("WAREHOUSE_CREATED"), eq("warehouses"), eq(8L), nullable(String.class), eq("WS-1"));
        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).saveAndFlush(captor.capture());
        assertEquals(COMPANY, captor.getValue().getCompanyId());
        assertEquals(BRANCH, captor.getValue().getBranchId());
    }

    @Test
    @DisplayName("Company admin cannot attach another company's branch")
    void companyAdminCannotUseForeignBranch() {
        Branch foreign = branch(OTHER_BRANCH, OTHER_COMPANY);
        when(branchRepository.findById(OTHER_BRANCH)).thenReturn(Optional.of(foreign));
        AppUser unboundAdmin = user("admin", COMPANY, null, "COMPANY_ADMIN");
        stubTenant(unboundAdmin, false);
        WarehouseRequest request = request("WS-1", "Store", COMPANY, OTHER_BRANCH);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> warehouseService.create(request, "admin"));
        assertEquals("WAREHOUSE_BRANCH_INVALID", ex.getErrorCode());
        verify(warehouseRepository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), eq("WAREHOUSE_CREATED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Branch manager creates warehouse in own branch")
    void branchManagerCreatesOwnWarehouse() {
        AppUser manager = user("mgr", COMPANY, BRANCH, "BRANCH_MANAGER");
        stubTenant(manager, false);
        WarehouseResponse response = warehouseService.create(request("WS-2", "Store", COMPANY, BRANCH), "mgr");
        assertEquals(BRANCH, response.getBranchId());
        verify(auditService).log(eq("mgr"), eq("WAREHOUSE_CREATED"), eq("warehouses"), eq(8L), nullable(String.class), eq("WS-2"));
    }

    @Test
    @DisplayName("Branch manager cannot create warehouse in another branch")
    void branchManagerCannotCreateOtherBranch() {
        AppUser manager = user("mgr", COMPANY, BRANCH, "BRANCH_MANAGER");
        stubTenant(manager, false);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> warehouseService.create(request("WS-2", "Store", COMPANY, OTHER_BRANCH), "mgr"));
        assertTrue(ex.getMessage().contains("another branch"));
        verify(warehouseRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Super Admin can create a warehouse for a requested company")
    void superAdminCreatesWarehouse() {
        AppUser superAdmin = user("root", null, null, "SUPER_ADMIN");
        stubTenant(superAdmin, true);
        when(tenantAccess.resolveCompanyId(any())).thenReturn(COMPANY);
        WarehouseResponse response = warehouseService.create(request("WS-SA", "SA Store", COMPANY, BRANCH), "root");
        assertEquals(COMPANY, response.getCompanyId());
        verify(auditService).log(eq("root"), eq("WAREHOUSE_CREATED"), eq("warehouses"), eq(8L), nullable(String.class), eq("WS-SA"));
    }

    @Test
    @DisplayName("Driver cannot create a warehouse")
    void driverDeniedWarehouse() {
        AppUser driver = user("drv", COMPANY, BRANCH, "DRIVER");
        stubTenant(driver, false);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> warehouseService.create(request("WS-D", "Store", COMPANY, BRANCH), "drv"));
        assertTrue(ex.getMessage().contains("COMPANY_ADMIN"));
        verify(warehouseRepository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), eq("WAREHOUSE_CREATED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Duplicate warehouse code is rejected")
    void duplicateCode() {
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException(
                "dup", new SQLException("duplicate key uk_warehouses_company_branch_code")))
                .when(warehouseRepository).saveAndFlush(any());
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> warehouseService.create(request("WS-1", "Store", COMPANY, BRANCH), "admin"));
        assertEquals("WAREHOUSE_CODE_DUPLICATE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Warehouse update cannot move company or branch")
    void updateCannotMoveBranch() {
        WarehouseRequest request = request("WS-1", "Renamed", COMPANY, OTHER_BRANCH);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> warehouseService.update(8L, request, "admin"));
        assertEquals("WAREHOUSE_BRANCH_INVALID", ex.getErrorCode());
    }

    @Test
    @DisplayName("Soft delete is blocked while on-hand stock exists")
    void deleteBlockedByStock() {
        when(warehouseRepository.countPositiveStock(8L)).thenReturn(1L);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> warehouseService.delete(8L, "admin"));
        assertEquals("WAREHOUSE_NOT_EDITABLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Soft delete succeeds when no positive stock remains")
    void deleteWithoutStock() {
        when(warehouseRepository.countPositiveStock(8L)).thenReturn(0L);
        warehouseService.delete(8L, "admin");
        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(captor.capture());
        assertTrue(Boolean.TRUE.equals(captor.getValue().getIsDeleted()));
    }

    @Test
    @DisplayName("Inactive warehouse is not usable for opening stock")
    void inactiveWarehouseRejected() {
        Warehouse inactive = storedWarehouse();
        inactive.setStatus("INACTIVE");
        when(warehouseRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(inactive));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> warehouseService.requireUsable(8L, companyAdmin));
        assertEquals("WAREHOUSE_INACTIVE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Warehouse list query uses a single fetch join")
    void warehouseListQueryIsBounded() throws Exception {
        String jpql = WarehouseRepository.class.getMethod("findDetailsByIds", java.util.Collection.class)
                .getAnnotation(Query.class).value();
        assertTrue(jpql.contains("JOIN FETCH w.branch"));
        assertEquals(1, jpql.split("JOIN FETCH").length - 1);
    }

    @Test
    @DisplayName("Phase 1-8 services do not depend on inventory")
    void noInventoryCoupling() {
        for (Class<?> type : List.of(
                SparePartService.class,
                WorkOrderService.class,
                WorkOrderLineService.class,
                WorkOrderFinancialPosting.class,
                VehicleMaintenanceStateService.class,
                TripService.class,
                MaintenanceRequestService.class)) {
            for (Field field : type.getDeclaredFields()) {
                String name = field.getType().getName();
                assertFalse(name.contains("Warehouse"), type.getSimpleName());
                assertFalse(name.contains("Inventory"), type.getSimpleName());
                assertFalse(name.contains("WarehouseStock"), type.getSimpleName());
            }
        }
    }

    private WarehouseRequest request(String code, String name, Long companyId, Long branchId) {
        WarehouseRequest request = new WarehouseRequest();
        request.setCode(code);
        request.setName(name);
        request.setCompanyId(companyId);
        request.setBranchId(branchId);
        request.setStatus("ACTIVE");
        return request;
    }

    private Warehouse storedWarehouse() {
        Warehouse row = new Warehouse();
        row.setId(8L);
        row.setCode("WS-1");
        row.setName("Main Workshop Store");
        row.setStatus("ACTIVE");
        row.setCompanyId(COMPANY);
        row.setBranchId(BRANCH);
        row.setIsDeleted(false);
        row.setBranch(branch);
        row.setVersion(0);
        return row;
    }

    private Branch branch(Long id, Long companyId) {
        Branch row = new Branch();
        row.setId(id);
        row.setCompanyId(companyId);
        row.setCode("BR-" + id);
        row.setName("Branch " + id);
        row.setIsDeleted(false);
        row.setStatus("ACTIVE");
        return row;
    }

    private void stubTenant(AppUser user, boolean superAdmin) {
        when(tenantAccess.requireCurrentUser()).thenReturn(user);
        when(tenantAccess.isSuperAdmin(user)).thenReturn(superAdmin);
        when(tenantAccess.resolveCompanyId(any())).thenReturn(user.getCompanyId() != null ? user.getCompanyId() : COMPANY);
        org.mockito.Mockito.doAnswer(inv -> {
            Long companyId = inv.getArgument(0);
            if (!superAdmin && (companyId == null || user.getCompanyId() == null || !companyId.equals(user.getCompanyId()))) {
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
