package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.VehicleDriverAssignmentRepository;
import com.transport.erp.repository.VehicleRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VehicleDriverAssignmentTest {

    @Mock
    private VehicleDriverAssignmentRepository assignmentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private TenantAccessService tenantAccess;

    @Mock
    private TenantParentAccess parentAccess;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private VehicleDriverAssignmentService assignmentService;

    private Vehicle vehicle;
    private Driver driver;
    private VehicleDriverAssignment activeAssignment;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setCode("VEH-10");
        vehicle.setName("TATA Truck");
        vehicle.setCompanyId(3L);
        vehicle.setBranchId(1L);
        vehicle.setStatus("ACTIVE");
        vehicle.setIsDeleted(false);

        driver = new Driver();
        driver.setId(20L);
        driver.setCode("DRV-20");
        driver.setName("John Driver");
        driver.setCompanyId(3L);
        driver.setBranchId(1L);
        driver.setStatus("ACTIVE");
        driver.setIsDeleted(false);

        activeAssignment = new VehicleDriverAssignment();
        activeAssignment.setId(100L);
        activeAssignment.setVehicle(vehicle);
        activeAssignment.setDriver(driver);
        activeAssignment.setAssignmentDate(LocalDate.now());
        activeAssignment.setStatus("ACTIVE");
        activeAssignment.setCompanyId(3L);
        activeAssignment.setBranchId(1L);
        activeAssignment.setIsDeleted(false);
    }

    @Test
    @DisplayName("1. Successful Vehicle-Driver Assignment")
    void testSuccessfulVehicleDriverAssignment() {
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(10L, 20L)).thenReturn(Optional.empty());
        when(assignmentRepository.findActiveByVehicleIdForUpdate(10L)).thenReturn(Optional.empty());
        when(assignmentRepository.findActiveByDriverIdForUpdate(20L)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(VehicleDriverAssignment.class))).thenAnswer(i -> i.getArgument(0));

        VehicleDriverAssignment saved = assignmentService.assignDriver(10L, 20L, "admin");

        assertNotNull(saved);
        assertEquals(10L, saved.getVehicle().getId());
        assertEquals(20L, saved.getDriver().getId());
        assertNull(saved.getRemovalDate());
        assertEquals("ACTIVE", saved.getStatus());
        verify(auditService).log(eq("admin"), eq("VEHICLE_DRIVER_ASSIGNED"), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("2. Duplicate Vehicle Assignment Throws VEHICLE_ALREADY_ASSIGNED")
    void testDuplicateVehicleAssignment_ThrowsException() {
        Driver anotherDriver = new Driver();
        anotherDriver.setId(21L);
        anotherDriver.setName("Other Driver");
        anotherDriver.setCompanyId(3L);

        VehicleDriverAssignment existingVehAssign = new VehicleDriverAssignment();
        existingVehAssign.setVehicle(vehicle);
        existingVehAssign.setDriver(anotherDriver);

        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(10L, 20L)).thenReturn(Optional.empty());
        when(assignmentRepository.findActiveByVehicleIdForUpdate(10L)).thenReturn(Optional.of(existingVehAssign));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                assignmentService.assignDriver(10L, 20L, "admin")
        );

        assertEquals("VEHICLE_ALREADY_ASSIGNED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("already actively assigned"));
    }

    @Test
    @DisplayName("3. Duplicate Driver Assignment Throws DRIVER_ALREADY_ASSIGNED")
    void testDuplicateDriverAssignment_ThrowsException() {
        Vehicle anotherVehicle = new Vehicle();
        anotherVehicle.setId(11L);
        anotherVehicle.setName("Other Truck");
        anotherVehicle.setCompanyId(3L);

        VehicleDriverAssignment existingDrvAssign = new VehicleDriverAssignment();
        existingDrvAssign.setVehicle(anotherVehicle);
        existingDrvAssign.setDriver(driver);

        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(10L, 20L)).thenReturn(Optional.empty());
        when(assignmentRepository.findActiveByVehicleIdForUpdate(10L)).thenReturn(Optional.empty());
        when(assignmentRepository.findActiveByDriverIdForUpdate(20L)).thenReturn(Optional.of(existingDrvAssign));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                assignmentService.assignDriver(10L, 20L, "admin")
        );

        assertEquals("DRIVER_ALREADY_ASSIGNED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("already actively assigned"));
    }

    @Test
    @DisplayName("4. Inactive Vehicle Status Throws VEHICLE_ASSIGNMENT_BLOCKED")
    void testInactiveVehicle_ThrowsException() {
        vehicle.setStatus("SUSPENDED");
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(driver));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                assignmentService.assignDriver(10L, 20L, "admin")
        );

        assertEquals("VEHICLE_ASSIGNMENT_BLOCKED", ex.getErrorCode());
    }

    @Test
    @DisplayName("5. Inactive Driver Status Throws DRIVER_ASSIGNMENT_BLOCKED")
    void testInactiveDriver_ThrowsException() {
        driver.setStatus("ON_LEAVE");
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(driver));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                assignmentService.assignDriver(10L, 20L, "admin")
        );

        assertEquals("DRIVER_ASSIGNMENT_BLOCKED", ex.getErrorCode());
    }

    @Test
    @DisplayName("6. Company Mismatch Throws VEHICLE_DRIVER_COMPANY_MISMATCH")
    void testCompanyMismatch_ThrowsException() {
        driver.setCompanyId(99L);
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(driver));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                assignmentService.assignDriver(10L, 20L, "admin")
        );

        assertEquals("VEHICLE_DRIVER_COMPANY_MISMATCH", ex.getErrorCode());
    }

    @Test
    @DisplayName("7. Branch Mismatch Throws VEHICLE_DRIVER_BRANCH_MISMATCH")
    void testBranchMismatch_ThrowsException() {
        driver.setBranchId(99L);
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(driver));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                assignmentService.assignDriver(10L, 20L, "admin")
        );

        assertEquals("VEHICLE_DRIVER_BRANCH_MISMATCH", ex.getErrorCode());
    }

    @Test
    @DisplayName("8. Successful Unassign Driver")
    void testUnassignDriver_Succeeds() {
        when(parentAccess.requireVehicle(10L)).thenReturn(vehicle);
        when(assignmentRepository.findActiveByVehicleIdForUpdate(10L)).thenReturn(Optional.of(activeAssignment));
        when(assignmentRepository.save(any(VehicleDriverAssignment.class))).thenAnswer(i -> i.getArgument(0));

        VehicleDriverAssignment unassigned = assignmentService.unassignDriver(10L, "admin");

        assertNotNull(unassigned.getRemovalDate());
        assertEquals("CLOSED", unassigned.getStatus());
        verify(auditService).log(eq("admin"), eq("VEHICLE_DRIVER_UNASSIGNED"), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("9. Unassigning Closed Vehicle Throws ASSIGNMENT_ALREADY_CLOSED")
    void testUnassignAlreadyClosed_ThrowsException() {
        when(parentAccess.requireVehicle(10L)).thenReturn(vehicle);
        when(assignmentRepository.findActiveByVehicleIdForUpdate(10L)).thenReturn(Optional.empty());

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                assignmentService.unassignDriver(10L, "admin")
        );

        assertEquals("ASSIGNMENT_ALREADY_CLOSED", ex.getErrorCode());
    }
}
