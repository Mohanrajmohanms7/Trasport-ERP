package com.transport.erp.service;

import com.transport.erp.dto.ServiceHistoryResponse;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Supplier;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleServiceLog;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.VehicleServiceLogRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VehicleServiceHistoryServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private VehicleServiceLogRepository serviceLogRepository;
    @Mock private TenantAccessService tenantAccess;
    @Mock private TenantParentAccess parentAccess;

    @InjectMocks private VehicleServiceHistoryService service;

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId(8L);
        vehicle.setCode("PM-OD-01");
        vehicle.setCompanyId(1L);
        vehicle.setBranchId(1L);
        vehicle.setIsDeleted(false);
        AppUser user = user(1L);
        when(tenantAccess.requireCurrentUser()).thenReturn(user);
        when(tenantAccess.isSuperAdmin(user)).thenReturn(false);
        when(parentAccess.requireVehicle(8L)).thenReturn(vehicle);
    }

    @Test
    @DisplayName("History merges completed work orders and service logs, newest first")
    void mergesAndOrdersSources() {
        WorkOrder older = completedOrder(3L, LocalDate.of(2026, 1, 2), new BigDecimal("80.00"));
        WorkOrder newer = completedOrder(9L, LocalDate.of(2026, 9, 20), new BigDecimal("350.00"));
        VehicleServiceLog log = new VehicleServiceLog();
        log.setId(4L);
        log.setVehicle(vehicle);
        log.setCompanyId(1L);
        log.setStatus("APPROVED");
        log.setServiceType("OIL_CHANGE");
        log.setName("Legacy oil");
        log.setServiceDate(LocalDate.of(2026, 5, 1));
        log.setCost(new BigDecimal("90.00"));
        log.setWorkshop("Roadside");
        when(workOrderRepository.findCompletedForVehicle(1L, 8L)).thenReturn(List.of(older, newer));
        when(serviceLogRepository.findActiveHistory(1L, 8L)).thenReturn(List.of(log));

        Page<ServiceHistoryResponse> page = service.history(8L, PageRequest.of(0, 10));

        assertEquals(3, page.getTotalElements());
        assertEquals("WORK_ORDER", page.getContent().get(0).getSourceType());
        assertEquals(9L, page.getContent().get(0).getSourceId());
        assertEquals("WO-000009", page.getContent().get(0).getWorkOrderNumber());
        assertEquals(0, new BigDecimal("350.00").compareTo(page.getContent().get(0).getFinancialAmount()));
        assertNull(page.getContent().get(0).getOperationalCost());
        assertEquals("SERVICE_LOG", page.getContent().get(1).getSourceType());
        assertEquals("Roadside", page.getContent().get(1).getSupplierName());
        assertEquals("WORK_ORDER", page.getContent().get(2).getSourceType());
        assertEquals(LocalDate.of(2026, 9, 20), page.getContent().get(0).getServiceDate());
        verify(workOrderRepository, times(1)).findCompletedForVehicle(1L, 8L);
        verify(serviceLogRepository, times(1)).findActiveHistory(1L, 8L);
        verify(serviceLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Another company vehicle is rejected")
    void companyIsolation() {
        when(parentAccess.requireVehicle(8L)).thenThrow(new AccessDeniedException("Access denied to another company's data"));
        assertThrows(AccessDeniedException.class, () -> service.history(8L, PageRequest.of(0, 10)));
        verify(workOrderRepository, never()).findCompletedForVehicle(any(), any());
    }

    @Test
    @DisplayName("Another branch vehicle is rejected")
    void branchIsolation() {
        vehicle.setBranchId(2L);
        assertThrows(AccessDeniedException.class, () -> service.history(8L, PageRequest.of(0, 10)));
        verify(workOrderRepository, never()).findCompletedForVehicle(any(), any());
    }

    @Test
    @DisplayName("Super admin can read another branch")
    void superAdminBypassesBranch() {
        AppUser admin = user(null);
        when(tenantAccess.requireCurrentUser()).thenReturn(admin);
        when(tenantAccess.isSuperAdmin(admin)).thenReturn(true);
        vehicle.setBranchId(2L);
        when(workOrderRepository.findCompletedForVehicle(1L, 8L)).thenReturn(List.of());
        when(serviceLogRepository.findActiveHistory(1L, 8L)).thenReturn(List.of());
        assertEquals(0, service.history(8L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    @DisplayName("History queries do not load part or labour lines")
    void historyQueriesDoNotLoadLines() throws Exception {
        String workOrders = WorkOrderRepository.class.getMethod("findCompletedForVehicle", Long.class, Long.class)
                .getAnnotation(Query.class).value();
        String logs = VehicleServiceLogRepository.class.getMethod("findActiveHistory", Long.class, Long.class)
                .getAnnotation(Query.class).value();
        assertTrue(workOrders.contains("JOIN FETCH w.vehicle"));
        assertTrue(workOrders.contains("LEFT JOIN FETCH w.supplier"));
        assertTrue(workOrders.contains("COMPLETED"));
        assertTrue(!workOrders.contains("WorkOrderPart"));
        assertTrue(!workOrders.contains("WorkOrderLabour"));
        assertTrue(logs.contains("JOIN FETCH l.vehicle"));
        assertTrue(logs.contains("CANCELLED"));
    }

    private WorkOrder completedOrder(Long id, LocalDate completedOn, BigDecimal actual) {
        WorkOrder order = new WorkOrder();
        order.setId(id);
        order.setWorkOrderNumber("WO-" + String.format("%06d", id));
        order.setStatus("COMPLETED");
        order.setCompanyId(1L);
        order.setBranchId(1L);
        order.setVehicle(vehicle);
        order.setMaintenanceType("OIL_CHANGE");
        order.setName("Oil service");
        order.setCompletedAt(completedOn.atStartOfDay());
        order.setActualCost(actual);
        order.setEstimatedCost(new BigDecimal("40.00"));
        order.setOdometerAtComplete(new BigDecimal("42500"));
        Supplier supplier = new Supplier();
        supplier.setName("ABC Motors");
        order.setSupplier(supplier);
        return order;
    }

    private AppUser user(Long branchId) {
        AppUser user = new AppUser();
        user.setId(2L);
        user.setCompanyId(1L);
        user.setBranchId(branchId);
        AppRole role = new AppRole();
        role.setCode("COMPANY_ADMIN");
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }
}
