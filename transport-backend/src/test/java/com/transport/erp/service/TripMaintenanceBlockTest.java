package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.Driver;
import com.transport.erp.model.Trip;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleDriverAssignment;
import com.transport.erp.repository.BookingRepository;
import com.transport.erp.repository.SalesInvoiceRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.repository.VehicleDriverAssignmentRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TripMaintenanceBlockTest {

    @Mock private TripRepository tripRepository;
    @Mock private SalesInvoiceRepository salesInvoiceRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VehicleDriverAssignmentRepository assignmentRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleMaintenanceStateService maintenanceState;
    @Mock private TenantAccessService tenantAccess;
    @Mock private BusinessDependencyValidationService validationService;
    @Mock private AuditService auditService;
    @Mock private AppSettingService settingService;

    @InjectMocks
    private TripService tripService;

    @BeforeEach
    void setUp() {
        when(settingService.getByKey(any())).thenReturn(Optional.empty());
        when(tenantAccess.resolveCompanyId(nullable(Long.class))).thenReturn(1L);
        when(tenantAccess.resolveBranchId(nullable(Long.class))).thenReturn(1L);
        when(assignmentRepository.findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(anyLong(), anyLong()))
                .thenReturn(Optional.of(new VehicleDriverAssignment()));
        when(vehicleRepository.findByIdForUpdate(anyLong())).thenAnswer(inv -> {
            Vehicle locked = new Vehicle();
            locked.setId(inv.getArgument(0));
            locked.setCompanyId(1L);
            locked.setIsDeleted(false);
            return Optional.of(locked);
        });
        when(maintenanceState.isVehicleUnderMaintenance(anyLong())).thenReturn(false);
        when(tripRepository.save(any())).thenAnswer(inv -> {
            Trip saved = inv.getArgument(0);
            saved.setId(70L);
            return saved;
        });
        when(tenantAccess.isSuperAdmin(any())).thenReturn(true);
    }

    @Test
    @DisplayName("A new trip is allowed when the vehicle has no IN_PROGRESS work order")
    void normalVehicleCanStartTrip() {
        Trip saved = tripService.createTrip(trip(1L, 2L), "companyadmin");
        assertEquals("PLANNED", saved.getStatus());
        verify(maintenanceState).isVehicleUnderMaintenance(1L);
        verify(auditService).log(any(), org.mockito.ArgumentMatchers.eq("TRIP_PLANNED"), any(), any(), any(), any());
        verify(tenantAccess, never()).isSuperAdmin(any());
    }

    @Test
    @DisplayName("OPEN, COMPLETED, and CANCELLED work orders do not block because they are not IN_PROGRESS")
    void nonProgressWorkOrdersAllowTrip() {
        when(maintenanceState.isVehicleUnderMaintenance(1L)).thenReturn(false);
        tripService.createTrip(trip(1L, 2L), "admin");
        verify(tripRepository).save(any());
    }

    @Test
    @DisplayName("An IN_PROGRESS work order rejects a new trip and does not persist it")
    void inProgressWorkOrderBlocksNewTrip() {
        when(maintenanceState.isVehicleUnderMaintenance(1L)).thenReturn(true);
        Trip request = trip(1L, 2L);
        request.setCompanyId(2L);
        request.setBranchId(9L);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> tripService.createTrip(request, "superadmin"));
        assertEquals("VEHICLE_UNDER_MAINTENANCE", ex.getErrorCode());
        verify(tripRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
        verify(tenantAccess, never()).isSuperAdmin(any());
    }

    @Test
    @DisplayName("A repeated create is checked again and still rejected")
    void retryDoesNotBypassMaintenance() {
        when(maintenanceState.isVehicleUnderMaintenance(1L)).thenReturn(true);
        assertThrows(BusinessValidationException.class, () -> tripService.createTrip(trip(1L, 2L), "admin"));
        assertThrows(BusinessValidationException.class, () -> tripService.createTrip(trip(1L, 2L), "admin"));
        verify(maintenanceState, times(2)).isVehicleUnderMaintenance(1L);
        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("The maintenance check runs after the vehicle row lock and ignores client vehicle status")
    void checkIsServerSideAfterLock() {
        when(maintenanceState.isVehicleUnderMaintenance(5L)).thenReturn(true);
        Trip request = trip(5L, 2L);
        request.getVehicle().setStatus("ACTIVE");
        assertThrows(BusinessValidationException.class, () -> tripService.createTrip(request, "branch-manager"));
        InOrder order = inOrder(vehicleRepository, maintenanceState);
        order.verify(vehicleRepository).findByIdForUpdate(5L);
        order.verify(maintenanceState).isVehicleUnderMaintenance(5L);
        assertFalse(hasField(Trip.class, "underMaintenance"));
    }

    @Test
    @DisplayName("A missing driver assignment is still rejected before maintenance state is considered")
    void assignmentStillRequired() {
        when(assignmentRepository.findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(1L, 2L))
                .thenReturn(Optional.empty());
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> tripService.createTrip(trip(1L, 2L), "driver"));
        assertEquals("DRIVER_VEHICLE_ASSIGNMENT_REQUIRED", ex.getErrorCode());
        verify(maintenanceState, never()).isVehicleUnderMaintenance(any());
        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dispatch, completion, and an edit that keeps the same vehicle stay available")
    void existingTripIsNotRetroactivelyBlocked() {
        when(maintenanceState.isVehicleUnderMaintenance(anyLong())).thenReturn(true);
        Trip existing = stored(trip(1L, 2L));

        Trip dispatched = tripService.dispatchTrip(existing.getId(), "admin");
        assertEquals("DISPATCHED", dispatched.getStatus());

        Trip completed = tripService.completeTrip(existing.getId(), "admin");
        assertEquals("COMPLETED", completed.getStatus());

        Trip edited = tripService.updateTrip(existing.getId(), trip(1L, 2L), "admin");
        assertEquals(1L, edited.getVehicle().getId());
        verify(maintenanceState, never()).isVehicleUnderMaintenance(any());
        verify(tripRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Assigning a different vehicle that is under maintenance is rejected")
    void reassignmentOntoMaintenanceVehicleIsBlocked() {
        Trip existing = stored(trip(1L, 2L));
        when(maintenanceState.isVehicleUnderMaintenance(8L)).thenReturn(true);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> tripService.updateTrip(existing.getId(), trip(8L, 2L), "admin"));
        assertEquals("VEHICLE_UNDER_MAINTENANCE", ex.getErrorCode());
        verify(tripRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trip blocking does not depend on accounting or maintenance-request services")
    void noAccountingDependency() {
        for (Field field : TripService.class.getDeclaredFields()) {
            String type = field.getType().getName();
            assertFalse(type.contains("Journal"));
            assertFalse(type.contains("Expense"));
            assertFalse(type.contains("WorkOrderFinancialPosting"));
            assertFalse(type.contains("MaintenanceRequest"));
            assertFalse(type.contains("VehicleOdometer"));
            assertFalse(type.contains("VehicleServiceLog"));
        }
        for (Field field : VehicleMaintenanceStateService.class.getDeclaredFields()) {
            String type = field.getType().getName();
            assertFalse(type.contains("Journal"));
            assertFalse(type.contains("Expense"));
            assertFalse(type.contains("WorkOrderFinancialPosting"));
            assertFalse(type.contains("MaintenanceRequest"));
        }
    }

    private Trip stored(Trip trip) {
        trip.setId(70L);
        trip.setIsDeleted(false);
        trip.setStatus("PLANNED");
        when(tripRepository.findById(70L)).thenReturn(Optional.of(trip));
        return trip;
    }

    private Trip trip(Long vehicleId, Long driverId) {
        Trip trip = new Trip();
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setCompanyId(1L);
        vehicle.setStatus("ACTIVE");
        Driver driver = new Driver();
        driver.setId(driverId);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setCompanyId(1L);
        trip.setBranchId(1L);
        return trip;
    }

    private boolean hasField(Class<?> type, String name) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
