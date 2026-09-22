package com.transport.erp.service;

import com.transport.erp.dto.WorkOrderCancelRequest;
import com.transport.erp.dto.WorkOrderCompleteRequest;
import com.transport.erp.dto.WorkOrderCreateRequest;
import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.dto.WorkOrderUpdateRequest;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.LookupValue;
import com.transport.erp.model.MaintenanceRule;
import com.transport.erp.model.Supplier;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleMaintenanceBaseline;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.LookupValueRepository;
import com.transport.erp.repository.MaintenanceRuleRepository;
import com.transport.erp.repository.SupplierRepository;
import com.transport.erp.repository.VehicleMaintenanceBaselineRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import jakarta.persistence.EntityManager;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderServiceTest {

    private static final Long COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;
    private static final Long VEHICLE_ID = 5L;
    private static final Long RULE_ID = 7L;

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private MaintenanceRuleRepository ruleRepository;
    @Mock private VehicleMaintenanceBaselineRepository baselineRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private TenantAccessService tenantAccess;
    @Mock private TenantParentAccess parentAccess;
    @Mock private AuditService auditService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private WorkOrderService service;

    private AppUser companyAdmin;
    private final AtomicReference<WorkOrder> persisted = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        companyAdmin = user("admin", COMPANY, 1L, "COMPANY_ADMIN");
        stubTenant(companyAdmin, false);
        when(workOrderRepository.saveAndFlush(any())).thenAnswer(inv -> {
            WorkOrder order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(10L);
            }
            persisted.set(order);
            return order;
        });
        when(workOrderRepository.save(any())).thenAnswer(inv -> {
            WorkOrder order = inv.getArgument(0);
            persisted.set(order);
            return order;
        });
        when(workOrderRepository.findDetailById(anyLong())).thenAnswer(inv -> Optional.ofNullable(persisted.get()));
    }

    @Test
    @DisplayName("OPEN to IN_PROGRESS sets started timestamp and audits once")
    void startFromOpen() {
        WorkOrder order = stored(openOrder());
        WorkOrderResponse response = service.start(order.getId(), "admin");
        assertEquals(WorkOrderService.STATUS_IN_PROGRESS, response.getStatus());
        assertEquals(WorkOrderService.STATUS_IN_PROGRESS, order.getStatus());
        assertTrue(order.getStartedAt() != null);
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_STARTED"), eq("work_orders"), eq(10L), isNull(), any());
    }

    @Test
    @DisplayName("IN_PROGRESS to COMPLETED copies authoritative odometer and does not touch baselines")
    void completeFromInProgress() {
        WorkOrder order = stored(openOrder());
        order.setStatus(WorkOrderService.STATUS_IN_PROGRESS);
        order.setOdometerAtOpen(new BigDecimal("51000"));
        Vehicle vehicle = order.getVehicle();
        vehicle.setCurrentOdometerKm(new BigDecimal("40000"));
        when(vehicleRepository.findByIdForUpdate(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        WorkOrderCompleteRequest request = new WorkOrderCompleteRequest();
        request.setCompletionNotes("Replaced filter");
        request.setActualCost(null);
        WorkOrderResponse response = service.complete(10L, request, "admin");

        assertEquals(WorkOrderService.STATUS_COMPLETED, response.getStatus());
        assertEquals(0, new BigDecimal("40000").compareTo(response.getOdometerAtComplete()));
        assertEquals("Replaced filter", response.getCompletionNotes());
        assertNull(response.getActualCost());
        assertEquals("admin", response.getCompletedBy());
        verify(entityManager).refresh(vehicle);
        verify(vehicleRepository, never()).save(any());
        verify(vehicleRepository, never()).saveAndFlush(any());
        verifyNoInteractions(baselineRepository);
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_COMPLETED"), eq("work_orders"), eq(10L), isNull(), any());
    }

    @Test
    @DisplayName("Completion copies a null authoritative odometer")
    void completeAllowsNullOdometer() {
        WorkOrder order = stored(openOrder());
        order.setStatus(WorkOrderService.STATUS_IN_PROGRESS);
        order.getVehicle().setCurrentOdometerKm(null);
        when(vehicleRepository.findByIdForUpdate(VEHICLE_ID)).thenReturn(Optional.of(order.getVehicle()));
        WorkOrderCompleteRequest request = new WorkOrderCompleteRequest();
        request.setCompletionNotes("No reading on vehicle");
        WorkOrderResponse response = service.complete(10L, request, "admin");
        assertNull(response.getOdometerAtComplete());
    }

    @Test
    @DisplayName("OPEN and IN_PROGRESS can be cancelled; COMPLETED cannot")
    void cancelTransitions() {
        WorkOrder open = stored(openOrder());
        WorkOrderCancelRequest request = new WorkOrderCancelRequest();
        request.setCancellationReason("Parts unavailable");
        WorkOrderResponse cancelled = service.cancel(open.getId(), request, "admin");
        assertEquals(WorkOrderService.STATUS_CANCELLED, cancelled.getStatus());
        assertEquals("Parts unavailable", cancelled.getCancellationReason());

        WorkOrder running = stored(openOrder());
        running.setStatus(WorkOrderService.STATUS_IN_PROGRESS);
        WorkOrderResponse cancelledRunning = service.cancel(running.getId(), request, "admin");
        assertEquals(WorkOrderService.STATUS_CANCELLED, cancelledRunning.getStatus());

        WorkOrder done = stored(openOrder());
        done.setStatus(WorkOrderService.STATUS_COMPLETED);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.cancel(done.getId(), request, "admin"));
        assertEquals("WORK_ORDER_INVALID_TRANSITION", ex.getErrorCode());
        verify(auditService, times(2)).log(eq("admin"), eq("WORK_ORDER_CANCELLED"), eq("work_orders"), eq(10L), isNull(), any());
    }

    @Test
    @DisplayName("Complete from OPEN and start from a non-open status are rejected without audit")
    void rejectIllegalTransitions() {
        WorkOrder open = stored(openOrder());
        WorkOrderCompleteRequest complete = new WorkOrderCompleteRequest();
        complete.setCompletionNotes("Too early");
        BusinessValidationException completeEx = assertThrows(BusinessValidationException.class,
                () -> service.complete(open.getId(), complete, "admin"));
        assertEquals("WORK_ORDER_INVALID_TRANSITION", completeEx.getErrorCode());

        open.setStatus(WorkOrderService.STATUS_IN_PROGRESS);
        BusinessValidationException startEx = assertThrows(BusinessValidationException.class,
                () -> service.start(open.getId(), "admin"));
        assertEquals("WORK_ORDER_INVALID_TRANSITION", startEx.getErrorCode());

        open.setStatus(WorkOrderService.STATUS_CANCELLED);
        BusinessValidationException startCancelled = assertThrows(BusinessValidationException.class,
                () -> service.start(open.getId(), "admin"));
        assertEquals("WORK_ORDER_INVALID_TRANSITION", startCancelled.getErrorCode());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Completion notes and cancellation reason are required before the row is locked")
    void requiredNotesAndReason() {
        BusinessValidationException notes = assertThrows(BusinessValidationException.class,
                () -> service.complete(10L, new WorkOrderCompleteRequest(), "admin"));
        assertEquals("WORK_ORDER_COMPLETION_NOTES_REQUIRED", notes.getErrorCode());

        BusinessValidationException reason = assertThrows(BusinessValidationException.class,
                () -> service.cancel(10L, new WorkOrderCancelRequest(), "admin"));
        assertEquals("WORK_ORDER_CANCEL_REASON_REQUIRED", reason.getErrorCode());
        verify(workOrderRepository, never()).findByIdForUpdate(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Preventive create accepts DUE, DUE_SOON, and OVERDUE and snapshots calculator output")
    void preventiveAcceptsActionableStatuses() {
        assertEquals("OVERDUE", createPreventive(new BigDecimal("51000")).getDueStatusAtCreation());
        assertEquals("DUE", createPreventive(new BigDecimal("50000")).getDueStatusAtCreation());
        assertEquals("DUE_SOON", createPreventive(new BigDecimal("49500")).getDueStatusAtCreation());
    }

    @Test
    @DisplayName("Preventive create rejects NOT_DUE and UNKNOWN")
    void preventiveRejectsNotDueAndUnknown() {
        BusinessValidationException notDue = assertThrows(BusinessValidationException.class,
                () -> createPreventive(new BigDecimal("41000")));
        assertEquals("WORK_ORDER_NOT_DUE", notDue.getErrorCode());

        BusinessValidationException unknown = assertThrows(BusinessValidationException.class,
                () -> createPreventive(null));
        assertEquals("WORK_ORDER_NOT_DUE", unknown.getErrorCode());
        verify(workOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Duplicate preventive cycle is rejected after the vehicle lock")
    void duplicatePreventiveRejectedAfterLock() {
        stubPreventiveGraph(new BigDecimal("51000"));
        when(workOrderRepository.countPreventiveCycle(eq(COMPANY), eq(VEHICLE_ID), eq(RULE_ID), any(), any()))
                .thenReturn(1L);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.create(preventiveRequest(), "admin"));
        assertEquals("WORK_ORDER_DUPLICATE_OPEN", ex.getErrorCode());
        InOrder order = inOrder(vehicleRepository, workOrderRepository);
        order.verify(vehicleRepository).findByIdForUpdate(VEHICLE_ID);
        order.verify(workOrderRepository).countPreventiveCycle(eq(COMPANY), eq(VEHICLE_ID), eq(RULE_ID), any(), any());
        verify(workOrderRepository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("A cancelled cycle is not a duplicate, and a manual job does not block preventive work")
    void cancelledAndManualDoNotBlock() throws Exception {
        Query query = WorkOrderRepository.class
                .getMethod("countPreventiveCycle", Long.class, Long.class, Long.class, BigDecimal.class, LocalDate.class)
                .getAnnotation(Query.class);
        String jpql = query.value();
        assertTrue(jpql.contains("'OPEN'"));
        assertTrue(jpql.contains("'IN_PROGRESS'"));
        assertTrue(jpql.contains("'COMPLETED'"));
        assertFalse(jpql.contains("CANCELLED"));
        assertTrue(jpql.contains("IS NULL"));

        stubPreventiveGraph(new BigDecimal("51000"));
        when(workOrderRepository.countPreventiveCycle(any(), any(), any(), any(), any())).thenReturn(0L);
        WorkOrderResponse created = service.create(preventiveRequest(), "admin");
        assertEquals(WorkOrderService.STATUS_OPEN, created.getStatus());

        LookupValue lookup = new LookupValue();
        lookup.setStatus("ACTIVE");
        when(lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(COMPANY, "MAINTENANCE_TYPE", "OIL_CHANGE"))
                .thenReturn(Optional.of(lookup));
        WorkOrderCreateRequest manual = preventiveRequest();
        manual.setSource("MANUAL");
        manual.setMaintenanceRuleId(null);
        manual.setMaintenanceType("OIL_CHANGE");
        WorkOrderResponse manualResult = service.create(manual, "admin");
        assertEquals("MANUAL", manualResult.getSource());
        assertNull(manualResult.getMaintenanceRuleId());
        verify(workOrderRepository, times(1)).countPreventiveCycle(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("A different maintenance rule does not trip the duplicate check")
    void differentRuleDoesNotBlock() {
        stubPreventiveGraph(new BigDecimal("51000"));
        MaintenanceRule other = kmRule(8L);
        when(ruleRepository.findByIdAndIsDeletedFalse(8L)).thenReturn(Optional.of(other));
        when(baselineRepository.findByRule_IdAndVehicle_IdAndIsDeletedFalse(8L, VEHICLE_ID))
                .thenReturn(Optional.of(baseline()));
        when(workOrderRepository.countPreventiveCycle(eq(COMPANY), eq(VEHICLE_ID), eq(8L), any(), any()))
                .thenReturn(0L);
        WorkOrderCreateRequest request = preventiveRequest();
        request.setMaintenanceRuleId(8L);
        WorkOrderResponse response = service.create(request, "admin");
        assertEquals(8L, response.getMaintenanceRuleId());
        verify(workOrderRepository).countPreventiveCycle(eq(COMPANY), eq(VEHICLE_ID), eq(8L), any(), any());
    }

    @Test
    @DisplayName("Unique index collisions from concurrent preventive creates map to WORK_ORDER_DUPLICATE_OPEN")
    void concurrentPreventiveInsertRejected() {
        stubPreventiveGraph(new BigDecimal("51000"));
        when(workOrderRepository.countPreventiveCycle(any(), any(), any(), any(), any())).thenReturn(0L);
        doThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_work_orders_preventive_cycle\""))
                .when(workOrderRepository).saveAndFlush(any());
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.create(preventiveRequest(), "admin"));
        assertEquals("WORK_ORDER_DUPLICATE_OPEN", ex.getErrorCode());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Stale locked status rejects the transition")
    void transitionUnderLockRejectsStaleStatus() {
        WorkOrder order = stored(openOrder());
        order.setStatus(WorkOrderService.STATUS_COMPLETED);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.start(order.getId(), "admin"));
        assertEquals("WORK_ORDER_INVALID_TRANSITION", ex.getErrorCode());
        verify(workOrderRepository).findByIdForUpdate(order.getId());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Client company, status, number, and odometer are ignored")
    void clientCompanyAndSnapshotsIgnored() {
        stubPreventiveGraph(new BigDecimal("51000"));
        when(workOrderRepository.countPreventiveCycle(any(), any(), any(), any(), any())).thenReturn(0L);
        WorkOrderCreateRequest request = preventiveRequest();
        request.setCompanyId(OTHER_COMPANY);
        request.setBranchId(99L);
        request.setStatus("COMPLETED");
        request.setWorkOrderNumber("HACK");
        request.setOdometerAtOpen(new BigDecimal("1"));
        request.setDueStatusAtCreation("NOT_DUE");
        request.setDueKm(new BigDecimal("1"));
        request.setBaselineLastServiceKm(new BigDecimal("1"));
        WorkOrderResponse response = service.create(request, "admin");
        assertEquals(COMPANY, response.getCompanyId());
        assertEquals(1L, response.getBranchId());
        assertEquals("OPEN", response.getStatus());
        assertEquals("WO-000010", response.getWorkOrderNumber());
        assertEquals(0, new BigDecimal("51000").compareTo(response.getOdometerAtOpen()));
        assertEquals("OVERDUE", response.getDueStatusAtCreation());
        assertEquals(0, new BigDecimal("50000").compareTo(response.getDueKm()));
        assertEquals(0, new BigDecimal("40000").compareTo(response.getBaselineLastServiceKm()));
    }

    @Test
    @DisplayName("SUPER_ADMIN list uses resolveCompanyId; tenant list ignores the client company")
    void companyResolution() {
        AppUser superAdmin = user("root", COMPANY, null, "SUPER_ADMIN");
        stubTenant(superAdmin, true);
        when(tenantAccess.resolveCompanyId(OTHER_COMPANY)).thenReturn(OTHER_COMPANY);
        when(workOrderRepository.searchIds(eq(OTHER_COMPANY), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        service.list(null, null, null, null, null, OTHER_COMPANY, PageRequest.of(0, 20));
        verify(workOrderRepository).searchIds(eq(OTHER_COMPANY), any(), any(), any(), any(), any(), any());

        stubTenant(companyAdmin, false);
        when(workOrderRepository.searchIds(eq(COMPANY), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        service.list(null, null, null, null, null, OTHER_COMPANY, PageRequest.of(0, 20));
        verify(workOrderRepository).searchIds(eq(COMPANY), isNull(), isNull(), isNull(), isNull(), eq(1L), any());
    }

    @Test
    @DisplayName("Company and branch isolation block create, read, update, and transition")
    void companyAndBranchIsolation() {
        Vehicle foreign = vehicle(new BigDecimal("51000"));
        foreign.setCompanyId(OTHER_COMPANY);
        when(vehicleRepository.findByIdForUpdate(VEHICLE_ID)).thenReturn(Optional.of(foreign));
        when(parentAccess.requireVehicle(VEHICLE_ID)).thenReturn(foreign);
        assertThrows(AccessDeniedException.class, () -> service.create(preventiveRequest(), "admin"));

        WorkOrder otherCompany = stored(openOrder());
        otherCompany.setCompanyId(OTHER_COMPANY);
        assertThrows(AccessDeniedException.class, () -> service.get(otherCompany.getId()));

        WorkOrder otherBranch = stored(openOrder());
        otherBranch.setBranchId(2L);
        AccessDeniedException branch = assertThrows(AccessDeniedException.class, () -> service.get(otherBranch.getId()));
        assertTrue(branch.getMessage().contains("another branch"));
        assertThrows(AccessDeniedException.class, () -> service.start(otherBranch.getId(), "admin"));

        WorkOrderUpdateRequest update = new WorkOrderUpdateRequest();
        update.setName("Nope");
        assertThrows(AccessDeniedException.class, () -> service.update(otherBranch.getId(), update, "admin"));
    }

    @Test
    @DisplayName("Supplier and assignee outside the vehicle company are rejected")
    void unauthorizedSupplierAndAssignee() {
        stubPreventiveGraph(new BigDecimal("51000"));
        when(workOrderRepository.countPreventiveCycle(any(), any(), any(), any(), any())).thenReturn(0L);
        Supplier supplier = new Supplier();
        supplier.setId(3L);
        supplier.setCompanyId(OTHER_COMPANY);
        supplier.setIsDeleted(false);
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(supplier));
        WorkOrderCreateRequest request = preventiveRequest();
        request.setSupplierId(3L);
        assertThrows(AccessDeniedException.class, () -> service.create(request, "admin"));

        when(supplierRepository.findById(4L)).thenReturn(Optional.empty());
        request.setSupplierId(4L);
        assertThrows(IllegalArgumentException.class, () -> service.create(request, "admin"));

        AppUser assignee = user("tech", OTHER_COMPANY, 1L, "COMPANY_ADMIN");
        assignee.setId(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(assignee));
        request.setSupplierId(null);
        request.setAssignedUserId(9L);
        assertThrows(AccessDeniedException.class, () -> service.create(request, "admin"));

        when(userRepository.findById(11L)).thenReturn(Optional.empty());
        request.setAssignedUserId(11L);
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> service.create(request, "admin"));
        assertTrue(missing.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("Manual create copies a null odometer and does not calculate due")
    void manualNullOdometer() {
        Vehicle vehicle = vehicle(null);
        when(vehicleRepository.findByIdForUpdate(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(VEHICLE_ID)).thenReturn(vehicle);
        LookupValue lookup = new LookupValue();
        lookup.setStatus("ACTIVE");
        when(lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(COMPANY, "MAINTENANCE_TYPE", "BRAKE_SERVICE"))
                .thenReturn(Optional.of(lookup));
        WorkOrderCreateRequest request = new WorkOrderCreateRequest();
        request.setVehicleId(VEHICLE_ID);
        request.setSource("MANUAL");
        request.setMaintenanceType("BRAKE_SERVICE");
        request.setName("Brake inspection");
        request.setPriority("HIGH");
        WorkOrderResponse response = service.create(request, "admin");
        assertNull(response.getOdometerAtOpen());
        assertNull(response.getDueStatusAtCreation());
        assertEquals("BRAKE_SERVICE", response.getMaintenanceType());
        verifyNoInteractions(baselineRepository);
        verify(workOrderRepository, never()).countPreventiveCycle(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("OPEN edits the allowed fields; IN_PROGRESS is limited; immutable fields are rejected")
    void updateRules() {
        WorkOrder open = stored(openOrder());
        WorkOrderUpdateRequest request = new WorkOrderUpdateRequest();
        request.setName("Renamed");
        request.setDiagnosis("Leak at seal");
        request.setPriority("HIGH");
        WorkOrderResponse updated = service.update(open.getId(), request, "admin");
        assertEquals("Renamed", updated.getName());
        assertEquals("HIGH", updated.getPriority());
        assertEquals("Leak at seal", updated.getDiagnosis());
        verify(auditService, times(1)).log(eq("admin"), eq("WORK_ORDER_UPDATED"), eq("work_orders"), eq(10L), isNull(), any());

        open.setStatus(WorkOrderService.STATUS_IN_PROGRESS);
        WorkOrderUpdateRequest blocked = new WorkOrderUpdateRequest();
        blocked.setName("No");
        BusinessValidationException notEditable = assertThrows(BusinessValidationException.class,
                () -> service.update(open.getId(), blocked, "admin"));
        assertEquals("WORK_ORDER_NOT_EDITABLE", notEditable.getErrorCode());

        WorkOrderUpdateRequest diagnosis = new WorkOrderUpdateRequest();
        diagnosis.setDiagnosis("Confirmed");
        diagnosis.setEstimatedCost(new BigDecimal("250"));
        WorkOrderResponse inProgress = service.update(open.getId(), diagnosis, "admin");
        assertEquals("Confirmed", inProgress.getDiagnosis());
        assertEquals(0, new BigDecimal("250").compareTo(inProgress.getEstimatedCost()));

        open.setStatus(WorkOrderService.STATUS_COMPLETED);
        BusinessValidationException closed = assertThrows(BusinessValidationException.class,
                () -> service.update(open.getId(), diagnosis, "admin"));
        assertEquals("WORK_ORDER_NOT_EDITABLE", closed.getErrorCode());

        open.setStatus(WorkOrderService.STATUS_OPEN);
        WorkOrderUpdateRequest vehicleChange = new WorkOrderUpdateRequest();
        vehicleChange.setVehicleId(99L);
        BusinessValidationException immutable = assertThrows(BusinessValidationException.class,
                () -> service.update(open.getId(), vehicleChange, "admin"));
        assertEquals("WORK_ORDER_NOT_EDITABLE", immutable.getErrorCode());
    }

    @Test
    @DisplayName("Deactivating a rule after creation leaves the existing work order readable")
    void deactivatedRuleDoesNotDestroyWorkOrder() {
        WorkOrder order = stored(openOrder());
        order.setSource("PREVENTIVE");
        order.setMaintenanceType("OIL_CHANGE");
        order.setDueStatusAtCreation("DUE");
        MaintenanceRule rule = kmRule(RULE_ID);
        rule.setStatus("INACTIVE");
        rule.setMaintenanceType("TYRE_CHANGE");
        order.setMaintenanceRule(rule);
        WorkOrderResponse response = service.get(order.getId());
        assertEquals("OIL_CHANGE", response.getMaintenanceType());
        assertEquals("DUE", response.getDueStatusAtCreation());
        assertEquals(RULE_ID, response.getMaintenanceRuleId());
        verify(ruleRepository, never()).findByIdAndIsDeletedFalse(any());
    }

    @Test
    @DisplayName("List and detail load associations in one fetch, not per row")
    void listAndDetailAvoidNPlusOne() {
        WorkOrder first = openOrder();
        first.setId(10L);
        WorkOrder second = openOrder();
        second.setId(11L);
        second.setName("Second");
        Pageable request = PageRequest.of(0, 20);
        when(workOrderRepository.searchIds(eq(COMPANY), isNull(), isNull(), isNull(), isNull(), eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(10L, 11L), request, 2));
        when(workOrderRepository.findDetailsByIds(List.of(10L, 11L))).thenReturn(List.of(second, first));

        Page<WorkOrderResponse> page = service.list(null, null, null, null, 2L, OTHER_COMPANY, request);
        assertEquals(2, page.getContent().size());
        assertEquals(10L, page.getContent().get(0).getId());
        assertEquals(11L, page.getContent().get(1).getId());
        verify(workOrderRepository, times(1)).searchIds(any(), any(), any(), any(), any(), any(), any());
        verify(workOrderRepository, times(1)).findDetailsByIds(any());
        verify(workOrderRepository, never()).findDetailById(any());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(workOrderRepository).searchIds(any(), any(), any(), any(), any(), any(), pageable.capture());
        Sort.Order sort = pageable.getValue().getSort().getOrderFor("openedAt");
        assertTrue(sort != null && sort.isDescending());

        persisted.set(first);
        service.get(10L);
        verify(workOrderRepository, times(1)).findDetailById(10L);
        verify(workOrderRepository, times(1)).findDetailsByIds(any());
    }

    @Test
    @DisplayName("Work order service does not depend on odometer, service-log, expense, or journal repositories")
    void noAccountingOrOdometerDependencies() {
        for (Field field : WorkOrderService.class.getDeclaredFields()) {
            String type = field.getType().getName();
            assertFalse(type.contains("VehicleOdometer"));
            assertFalse(type.contains("VehicleServiceLog"));
            assertFalse(type.contains("Expense"));
            assertFalse(type.contains("Journal"));
        }
    }

    @Test
    @DisplayName("Drivers cannot create work orders")
    void writeRoleRequired() {
        stubTenant(user("driver", COMPANY, 1L, "DRIVER"), false);
        assertThrows(AccessDeniedException.class, () -> service.create(preventiveRequest(), "driver"));
        verify(vehicleRepository, never()).findByIdForUpdate(any());
    }

    private WorkOrderResponse createPreventive(BigDecimal currentKm) {
        stubPreventiveGraph(currentKm);
        when(workOrderRepository.countPreventiveCycle(any(), any(), any(), any(), any())).thenReturn(0L);
        return service.create(preventiveRequest(), "admin");
    }

    private void stubPreventiveGraph(BigDecimal currentKm) {
        Vehicle vehicle = vehicle(currentKm);
        when(vehicleRepository.findByIdForUpdate(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(VEHICLE_ID)).thenReturn(vehicle);
        when(ruleRepository.findByIdAndIsDeletedFalse(RULE_ID)).thenReturn(Optional.of(kmRule(RULE_ID)));
        when(baselineRepository.findByRule_IdAndVehicle_IdAndIsDeletedFalse(RULE_ID, VEHICLE_ID))
                .thenReturn(Optional.of(baseline()));
    }

    private WorkOrderCreateRequest preventiveRequest() {
        WorkOrderCreateRequest request = new WorkOrderCreateRequest();
        request.setVehicleId(VEHICLE_ID);
        request.setSource("PREVENTIVE");
        request.setMaintenanceRuleId(RULE_ID);
        request.setName("Oil change");
        request.setPriority("NORMAL");
        return request;
    }

    private WorkOrder stored(WorkOrder order) {
        persisted.set(order);
        when(workOrderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(workOrderRepository.findDetailById(order.getId())).thenReturn(Optional.of(order));
        return order;
    }

    private WorkOrder openOrder() {
        WorkOrder order = new WorkOrder();
        order.setId(10L);
        order.setCode("WO-000010");
        order.setWorkOrderNumber("WO-000010");
        order.setName("Oil change");
        order.setStatus(WorkOrderService.STATUS_OPEN);
        order.setSource(WorkOrderService.SOURCE_MANUAL);
        order.setMaintenanceType("OIL_CHANGE");
        order.setPriority(WorkOrderService.PRIORITY_NORMAL);
        order.setCompanyId(COMPANY);
        order.setBranchId(1L);
        order.setVehicle(vehicle(new BigDecimal("51000")));
        order.setIsDeleted(false);
        return order;
    }

    private Vehicle vehicle(BigDecimal km) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setCompanyId(COMPANY);
        vehicle.setBranchId(1L);
        vehicle.setCode("PM-OD-01");
        vehicle.setName("Truck");
        vehicle.setCurrentOdometerKm(km);
        vehicle.setIsDeleted(false);
        return vehicle;
    }

    private MaintenanceRule kmRule(Long id) {
        MaintenanceRule rule = new MaintenanceRule();
        rule.setId(id);
        rule.setCompanyId(COMPANY);
        rule.setName("Engine oil");
        rule.setStatus("ACTIVE");
        rule.setIsDeleted(false);
        rule.setMaintenanceType("OIL_CHANGE");
        rule.setTriggerMode(MaintenanceDueCalculator.MODE_KM);
        rule.setIntervalKm(new BigDecimal("10000"));
        rule.setDueSoonKm(new BigDecimal("1000"));
        return rule;
    }

    private VehicleMaintenanceBaseline baseline() {
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setLastServiceKm(new BigDecimal("40000"));
        baseline.setLastServiceDate(LocalDate.of(2026, 1, 15));
        return baseline;
    }

    private void stubTenant(AppUser user, boolean superAdmin) {
        when(tenantAccess.requireCurrentUser()).thenReturn(user);
        when(tenantAccess.isSuperAdmin(user)).thenReturn(superAdmin);
        when(tenantAccess.isSuperAdmin()).thenReturn(superAdmin);
        if (superAdmin) {
            doNothing().when(tenantAccess).assertCompanyAccess(nullable(Long.class));
            doNothing().when(tenantAccess).assertOwned(nullable(Long.class));
            return;
        }
        doAnswer(inv -> {
            Long companyId = inv.getArgument(0);
            if (companyId == null || !companyId.equals(user.getCompanyId())) {
                throw new AccessDeniedException("Access denied to another company's data");
            }
            return null;
        }).when(tenantAccess).assertCompanyAccess(nullable(Long.class));
        doAnswer(inv -> {
            Long companyId = inv.getArgument(0);
            if (companyId == null || !companyId.equals(user.getCompanyId())) {
                throw new AccessDeniedException("Access denied to another company's data");
            }
            return null;
        }).when(tenantAccess).assertOwned(nullable(Long.class));
        when(tenantAccess.resolveCompanyId(any())).thenReturn(user.getCompanyId());
    }

    private AppUser user(String username, Long companyId, Long branchId, String roleCode) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername(username);
        user.setName(username);
        user.setCompanyId(companyId);
        user.setBranchId(branchId);
        AppRole role = new AppRole();
        role.setCode(roleCode);
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }
}
