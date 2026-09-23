package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceRequestCancelRequest;
import com.transport.erp.dto.MaintenanceRequestCreateRequest;
import com.transport.erp.dto.MaintenanceRequestResponse;
import com.transport.erp.dto.MaintenanceRequestReviewRequest;
import com.transport.erp.dto.MaintenanceRequestUpdateRequest;
import com.transport.erp.dto.WorkOrderCreateRequest;
import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.model.MaintenanceRequest;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.MaintenanceRequestRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintenanceRequestServiceTest {

    @Mock private MaintenanceRequestRepository requestRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderService workOrderService;
    @Mock private DriverVehicleAuthorizationService driverAuthorization;
    @Mock private TenantAccessService tenantAccess;
    @Mock private TenantParentAccess parentAccess;
    @Mock private AuditService auditService;
    @Mock private EntityManager entityManager;
    @InjectMocks private MaintenanceRequestService service;

    private AppUser admin;
    private AppUser driverUser;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        admin = user(1L, "admin", 1L, 1L, "COMPANY_ADMIN");
        driverUser = user(4L, "ram", 1L, 1L, "DRIVER");
        vehicle = vehicle(5L, 1L, 1L, new BigDecimal("42500"));
    }

    @Test
    void driverCreatesRequestForAssignedVehicle() {
        stubUser(driverUser, false);
        Driver driver = new Driver();
        driver.setId(9L);
        driver.setCompanyId(1L);
        driver.setName("Ram");
        when(driverAuthorization.isDriverRole(driverUser)).thenReturn(true);
        when(driverAuthorization.requireLinkedDriver(driverUser)).thenReturn(driver);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        AtomicReference<MaintenanceRequest> stored = new AtomicReference<>();
        when(requestRepository.saveAndFlush(any())).thenAnswer(inv -> {
            MaintenanceRequest row = inv.getArgument(0);
            row.setId(12L);
            stored.set(row);
            return row;
        });
        when(requestRepository.save(any())).thenAnswer(inv -> {
            stored.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(requestRepository.findDetailById(12L)).thenAnswer(inv -> Optional.of(stored.get()));

        MaintenanceRequestResponse response = service.create(createBody(), "ram");

        assertEquals("MR-000012", response.getRequestNumber());
        assertEquals("OPEN", response.getStatus());
        assertEquals(new BigDecimal("42500.00"), response.getReportedOdometerKm());
        assertEquals(4L, response.getRequestedByUserId());
        assertEquals(9L, response.getDriverId());
        verify(driverAuthorization).assertAuthorized(driverUser, driver, vehicle);
        verify(auditService).log(eq("ram"), eq("MAINTENANCE_REQUEST_CREATED"), eq("maintenance_requests"), eq(12L), isNull(), any());
        verifyNoInteractions(workOrderService);
    }

    @Test
    void missingDescriptionIsRejected() {
        stubUser(admin, false);
        when(driverAuthorization.isDriverRole(admin)).thenReturn(false);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        MaintenanceRequestCreateRequest body = createBody();
        body.setDescription(" ");
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> service.create(body, "admin"));
        assertEquals("MAINTENANCE_REQUEST_DESCRIPTION_REQUIRED", ex.getErrorCode());
    }

    @Test
    void missingTitleIsRejected() {
        stubUser(admin, false);
        when(driverAuthorization.isDriverRole(admin)).thenReturn(false);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        MaintenanceRequestCreateRequest body = createBody();
        body.setTitle(" ");
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> service.create(body, "admin"));
        assertEquals("MAINTENANCE_REQUEST_TITLE_REQUIRED", ex.getErrorCode());
    }

    @Test
    void invalidPriorityIsRejected() {
        stubUser(admin, false);
        when(driverAuthorization.isDriverRole(admin)).thenReturn(false);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        MaintenanceRequestCreateRequest body = createBody();
        body.setPriority("URGENT");
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> service.create(body, "admin"));
        assertEquals("MAINTENANCE_REQUEST_PRIORITY_INVALID", ex.getErrorCode());
    }

    @Test
    void deletedVehicleIsRejected() {
        stubUser(admin, false);
        vehicle.setIsDeleted(true);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.create(createBody(), "admin"));
        assertEquals("MAINTENANCE_REQUEST_VEHICLE_REQUIRED", ex.getErrorCode());
    }

    @Test
    void otherCompanyVehicleIsRejected() {
        stubUser(admin, false);
        vehicle.setCompanyId(2L);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        assertThrows(AccessDeniedException.class, () -> service.create(createBody(), "admin"));
        verifyNoInteractions(workOrderService);
    }

    @Test
    void otherBranchVehicleIsRejected() {
        stubUser(admin, false);
        vehicle.setBranchId(2L);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        assertThrows(AccessDeniedException.class, () -> service.create(createBody(), "admin"));
    }

    @Test
    void driverWithoutMappingIsRejected() {
        stubUser(driverUser, false);
        when(driverAuthorization.isDriverRole(driverUser)).thenReturn(true);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        when(driverAuthorization.requireLinkedDriver(driverUser)).thenThrow(new BusinessValidationException(
                "Driver Not Linked", "DRIVER_NOT_LINKED", "DRIVER_NOT_LINKED: missing", "Link the driver."));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.create(createBody(), "ram"));
        assertEquals("DRIVER_NOT_LINKED", ex.getErrorCode());
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void driverUnauthorizedVehicleIsRejected() {
        stubUser(driverUser, false);
        Driver driver = new Driver();
        driver.setId(9L);
        driver.setCompanyId(1L);
        when(driverAuthorization.isDriverRole(driverUser)).thenReturn(true);
        when(driverAuthorization.requireLinkedDriver(driverUser)).thenReturn(driver);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        doThrow(new BusinessValidationException(
                "Vehicle Not Authorized", "DRIVER_VEHICLE_NOT_AUTHORIZED", "DRIVER_VEHICLE_NOT_AUTHORIZED: no", "Pick another."))
                .when(driverAuthorization).assertAuthorized(driverUser, driver, vehicle);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.create(createBody(), "ram"));
        assertEquals("DRIVER_VEHICLE_NOT_AUTHORIZED", ex.getErrorCode());
    }

    @Test
    void openRequestCanBeUpdatedAndLaterStatesCannot() {
        stubUser(admin, false);
        when(driverAuthorization.isDriverRole(admin)).thenReturn(false);
        MaintenanceRequest row = openRow();
        when(requestRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(row));
        when(requestRepository.save(row)).thenReturn(row);
        when(requestRepository.findDetailById(12L)).thenReturn(Optional.of(row));
        MaintenanceRequestUpdateRequest update = new MaintenanceRequestUpdateRequest();
        update.setTitle("Brake noise");
        assertEquals("Brake noise", service.update(12L, update, "admin").getTitle());

        row.setStatus(MaintenanceRequestService.STATUS_UNDER_REVIEW);
        assertEquals("MAINTENANCE_REQUEST_NOT_EDITABLE",
                assertThrows(BusinessValidationException.class, () -> service.update(12L, update, "admin")).getErrorCode());
        row.setStatus(MaintenanceRequestService.STATUS_CONVERTED);
        assertEquals("MAINTENANCE_REQUEST_NOT_EDITABLE",
                assertThrows(BusinessValidationException.class, () -> service.update(12L, update, "admin")).getErrorCode());
        row.setStatus(MaintenanceRequestService.STATUS_CANCELLED);
        assertEquals("MAINTENANCE_REQUEST_NOT_EDITABLE",
                assertThrows(BusinessValidationException.class, () -> service.update(12L, update, "admin")).getErrorCode());
    }

    @Test
    void reviewApproveAndCancelFollowTheLifecycle() {
        stubUser(admin, false);
        MaintenanceRequest row = openRow();
        when(requestRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(row));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.findDetailById(12L)).thenReturn(Optional.of(row));

        assertEquals("UNDER_REVIEW", service.review(12L, new MaintenanceRequestReviewRequest(), "admin").getStatus());
        assertEquals("APPROVED", service.approve(12L, "admin").getStatus());
        row.setStatus(MaintenanceRequestService.STATUS_OPEN);
        MaintenanceRequestCancelRequest cancel = new MaintenanceRequestCancelRequest();
        cancel.setCancellationReason("Duplicate");
        assertEquals("CANCELLED", service.cancel(12L, cancel, "admin").getStatus());

        row.setStatus(MaintenanceRequestService.STATUS_UNDER_REVIEW);
        row.setCancellationReason(null);
        assertEquals("CANCELLED", service.cancel(12L, cancel, "admin").getStatus());

        row.setStatus(MaintenanceRequestService.STATUS_CONVERTED);
        assertEquals("MAINTENANCE_REQUEST_ALREADY_CONVERTED",
                assertThrows(BusinessValidationException.class, () -> service.cancel(12L, cancel, "admin")).getErrorCode());
        row.setStatus(MaintenanceRequestService.STATUS_CANCELLED);
        assertEquals("MAINTENANCE_REQUEST_INVALID_TRANSITION",
                assertThrows(BusinessValidationException.class, () -> service.approve(12L, "admin")).getErrorCode());
        assertEquals("MAINTENANCE_REQUEST_INVALID_TRANSITION",
                assertThrows(BusinessValidationException.class, () -> service.review(12L, null, "admin")).getErrorCode());
    }

    @Test
    void approvedRequestConvertsOnceAndCopiesContext() {
        stubUser(admin, false);
        MaintenanceRequest row = openRow();
        row.setStatus(MaintenanceRequestService.STATUS_APPROVED);
        when(requestRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(row));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.findDetailById(12L)).thenReturn(Optional.of(row));
        WorkOrderResponse created = new WorkOrderResponse();
        created.setId(77L);
        created.setWorkOrderNumber("WO-000077");
        when(workOrderService.create(any(), eq("admin"))).thenReturn(created);
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(77L);
        workOrder.setWorkOrderNumber("WO-000077");
        when(workOrderRepository.getReferenceById(77L)).thenReturn(workOrder);

        MaintenanceRequestResponse response = service.convert(12L, "admin");

        assertEquals("CONVERTED", response.getStatus());
        assertEquals(77L, response.getWorkOrderId());
        ArgumentCaptor<WorkOrderCreateRequest> captor = ArgumentCaptor.forClass(WorkOrderCreateRequest.class);
        verify(workOrderService).create(captor.capture(), eq("admin"));
        WorkOrderCreateRequest sent = captor.getValue();
        assertEquals(5L, sent.getVehicleId());
        assertEquals("MANUAL", sent.getSource());
        assertEquals("GENERAL_SERVICE", sent.getMaintenanceType());
        assertEquals("Brake noise during braking", sent.getDescription());
        assertEquals("Brake noise during braking", sent.getDiagnosis());
        assertEquals("HIGH", sent.getPriority());
        assertNull(sent.getEstimatedCost());
        verify(workOrderService, times(1)).create(any(), any());

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () -> service.convert(12L, "admin"));
        assertEquals("MAINTENANCE_REQUEST_ALREADY_CONVERTED", ex.getErrorCode());
        verify(workOrderService, times(1)).create(any(), any());
    }

    @Test
    void conversionFailureLeavesTheRequestApproved() {
        stubUser(admin, false);
        MaintenanceRequest row = openRow();
        row.setStatus(MaintenanceRequestService.STATUS_APPROVED);
        when(requestRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(row));
        when(workOrderService.create(any(), any())).thenThrow(new BusinessValidationException(
                "Invalid Type", "INVALID_MAINTENANCE_TYPE", "missing type", "Seed the lookup."));
        assertThrows(BusinessValidationException.class, () -> service.convert(12L, "admin"));
        assertEquals("APPROVED", row.getStatus());
        assertNull(row.getWorkOrder());
        verify(requestRepository, never()).save(any());
        verify(auditService, never()).log(any(), eq("MAINTENANCE_REQUEST_CONVERTED"), any(), any(), any(), any());
    }

    @Test
    void cancelledRequestCannotBeConverted() {
        stubUser(admin, false);
        MaintenanceRequest row = openRow();
        row.setStatus(MaintenanceRequestService.STATUS_CANCELLED);
        when(requestRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(row));
        assertEquals("MAINTENANCE_REQUEST_INVALID_TRANSITION",
                assertThrows(BusinessValidationException.class, () -> service.convert(12L, "admin")).getErrorCode());
        verifyNoInteractions(workOrderService);
    }

    @Test
    void superAdminBypassesBranch() {
        AppUser superAdmin = user(2L, "root", 1L, 1L, "SUPER_ADMIN");
        stubUser(superAdmin, true);
        vehicle.setBranchId(9L);
        when(driverAuthorization.isDriverRole(superAdmin)).thenReturn(false);
        when(vehicleRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(5L)).thenReturn(vehicle);
        when(requestRepository.saveAndFlush(any())).thenAnswer(inv -> {
            MaintenanceRequest row = inv.getArgument(0);
            row.setId(3L);
            return row;
        });
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MaintenanceRequest stored = openRow();
        stored.setId(3L);
        stored.setRequestNumber("MR-000003");
        stored.setBranchId(9L);
        when(requestRepository.findDetailById(3L)).thenReturn(Optional.of(stored));
        assertEquals(9L, service.create(createBody(), "root").getBranchId());
    }

    @Test
    void listUsesOneIdPageAndOneDetailQuery() {
        stubUser(admin, false);
        when(driverAuthorization.isDriverRole(admin)).thenReturn(false);
        when(tenantAccess.resolveCompanyId(null)).thenReturn(1L);
        when(requestRepository.searchIds(eq(1L), isNull(), isNull(), isNull(), isNull(), any(), any(), eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(12L)));
        MaintenanceRequest row = openRow();
        when(requestRepository.findDetailsByIds(List.of(12L))).thenReturn(List.of(row));
        Page<MaintenanceRequestResponse> page = service.list(null, null, null, null, null, null, null, null, Pageable.ofSize(20));
        assertEquals(1, page.getTotalElements());
        verify(requestRepository, times(1)).searchIds(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(requestRepository, times(1)).findDetailsByIds(any());
        verify(vehicleRepository, never()).findById(any());
    }

    @Test
    void serviceDoesNotPostAccounting() {
        for (Field field : MaintenanceRequestService.class.getDeclaredFields()) {
            String type = field.getType().getName();
            assertFalse(type.contains("Journal"));
            assertFalse(type.contains("Expense"));
            assertFalse(type.contains("WorkOrderFinancialPosting"));
            assertFalse(type.contains("VehicleServiceLog"));
            assertFalse(type.contains("VehicleOdometer"));
        }
    }

    private MaintenanceRequest savedRow() {
        MaintenanceRequest row = openRow();
        row.setRequestNumber("MR-000012");
        row.setCode("MR-000012");
        row.setReportedOdometerKm(new BigDecimal("42500.00"));
        return row;
    }

    private MaintenanceRequest openRow() {
        MaintenanceRequest row = new MaintenanceRequest();
        row.setId(12L);
        row.setCompanyId(1L);
        row.setBranchId(1L);
        row.setVehicle(vehicle);
        row.setRequestedBy(driverUser);
        row.setName("Brake issue");
        row.setDescription("Brake noise during braking");
        row.setPriority("HIGH");
        row.setStatus("OPEN");
        row.setRequestNumber("MR-000012");
        row.setRequestedAt(java.time.LocalDateTime.of(2026, 9, 23, 8, 0));
        row.setIsDeleted(false);
        return row;
    }

    private MaintenanceRequestCreateRequest createBody() {
        MaintenanceRequestCreateRequest body = new MaintenanceRequestCreateRequest();
        body.setVehicleId(5L);
        body.setTitle("Brake issue");
        body.setDescription("Brake noise during braking");
        body.setPriority("HIGH");
        return body;
    }

    private Vehicle vehicle(Long id, Long companyId, Long branchId, BigDecimal odometer) {
        Vehicle row = new Vehicle();
        row.setId(id);
        row.setCompanyId(companyId);
        row.setBranchId(branchId);
        row.setCode("PM-OD-01");
        row.setName("Haul truck");
        row.setCurrentOdometerKm(odometer);
        row.setIsDeleted(false);
        return row;
    }

    private void stubUser(AppUser user, boolean superAdmin) {
        when(tenantAccess.requireCurrentUser()).thenReturn(user);
        when(tenantAccess.isSuperAdmin(user)).thenReturn(superAdmin);
        when(tenantAccess.isSuperAdmin()).thenReturn(superAdmin);
        if (superAdmin) {
            return;
        }
        doAnswer(inv -> {
            Long companyId = inv.getArgument(0);
            if (companyId == null || !companyId.equals(user.getCompanyId())) {
                throw new AccessDeniedException("Access denied to another company's data");
            }
            return null;
        }).when(tenantAccess).assertCompanyAccess(any());
    }

    private AppUser user(Long id, String username, Long companyId, Long branchId, String role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setCompanyId(companyId);
        user.setBranchId(branchId);
        user.setIsDeleted(false);
        AppRole appRole = new AppRole();
        appRole.setCode(role);
        user.setRoles(new HashSet<>(Set.of(appRole)));
        return user;
    }
}
