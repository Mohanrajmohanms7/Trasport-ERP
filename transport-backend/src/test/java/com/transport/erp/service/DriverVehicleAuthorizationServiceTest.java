package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.model.Vehicle;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.VehicleDriverAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverVehicleAuthorizationServiceTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleDriverAssignmentRepository assignmentRepository;
    @InjectMocks
    private DriverVehicleAuthorizationService service;

    @Test
    void assignedVehicleIsAllowed() {
        AppUser user = user(4L, 1L);
        Driver driver = driver(9L, 1L, 4L);
        Vehicle vehicle = vehicle(5L, 1L);
        when(assignmentRepository.countActiveAuthorization(4L, 1L, 5L)).thenReturn(1L);
        service.assertAuthorized(user, driver, vehicle);
        verify(assignmentRepository).countActiveAuthorization(4L, 1L, 5L);
    }

    @Test
    void unassignedVehicleIsRejected() {
        when(assignmentRepository.countActiveAuthorization(4L, 1L, 5L)).thenReturn(0L);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.assertAuthorized(user(4L, 1L), driver(9L, 1L, 4L), vehicle(5L, 1L)));
        assertEquals("DRIVER_VEHICLE_NOT_AUTHORIZED", ex.getErrorCode());
    }

    @Test
    void anotherVehicleIsRejected() {
        when(assignmentRepository.countActiveAuthorization(4L, 1L, 8L)).thenReturn(0L);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.assertAuthorized(user(4L, 1L), driver(9L, 1L, 4L), vehicle(8L, 1L)));
        assertEquals("DRIVER_VEHICLE_NOT_AUTHORIZED", ex.getErrorCode());
    }

    @Test
    void otherCompanyVehicleIsRejected() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.assertAuthorized(user(4L, 1L), driver(9L, 1L, 4L), vehicle(5L, 2L)));
        assertEquals("DRIVER_VEHICLE_NOT_AUTHORIZED", ex.getErrorCode());
    }

    @Test
    void missingMappingIsRejected() {
        when(driverRepository.findByAppUserIdAndIsDeletedFalse(4L)).thenReturn(Optional.empty());
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.requireLinkedDriver(user(4L, 1L)));
        assertEquals("DRIVER_NOT_LINKED", ex.getErrorCode());
    }

    @Test
    void crossCompanyMappingIsRejected() {
        when(driverRepository.findByAppUserIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(driver(9L, 2L, 4L)));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> service.requireLinkedDriver(user(4L, 1L)));
        assertEquals("DRIVER_NOT_LINKED", ex.getErrorCode());
    }

    @Test
    void authorizedIdsComeFromActiveAssignmentsOnly() {
        when(driverRepository.findByAppUserIdAndIsDeletedFalse(4L)).thenReturn(Optional.of(driver(9L, 1L, 4L)));
        when(assignmentRepository.findActivelyAssignedVehicles(1L, 9L)).thenReturn(List.of(vehicle(5L, 1L)));
        assertEquals(List.of(5L), service.authorizedVehicleIds(user(4L, 1L)));
    }

    private AppUser user(Long id, Long companyId) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setCompanyId(companyId);
        AppRole role = new AppRole();
        role.setCode("DRIVER");
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }

    private Driver driver(Long id, Long companyId, Long appUserId) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setCompanyId(companyId);
        driver.setAppUserId(appUserId);
        driver.setIsDeleted(false);
        return driver;
    }

    private Vehicle vehicle(Long id, Long companyId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setCompanyId(companyId);
        vehicle.setIsDeleted(false);
        return vehicle;
    }
}
