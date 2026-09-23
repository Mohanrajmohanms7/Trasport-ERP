package com.transport.erp.service;

import com.transport.erp.dto.AuthorizedVehicleResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.model.Vehicle;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.VehicleDriverAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Resolves AppUser to Driver to the active rows in vehicle_driver_assignments.
 * Company or branch membership is not a substitute for that assignment.
 * A closed assignment (removal date set, or deleted) does not authorize a vehicle.
 * There is no historical-access rule, so a former assignment does not keep access.
 */
@Service
public class DriverVehicleAuthorizationService {

    static final String ROLE_DRIVER = "DRIVER";

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleDriverAssignmentRepository assignmentRepository;

    public boolean isDriverRole(AppUser user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream().anyMatch(role -> ROLE_DRIVER.equals(role.getCode()));
    }

    public boolean hasDriverRole(AppUser user) {
        return isDriverRole(user);
    }

    @Transactional(readOnly = true)
    public Driver requireLinkedDriver(AppUser user) {
        if (user == null || user.getId() == null) {
            throw notLinked();
        }
        Driver driver = driverRepository.findByAppUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(this::notLinked);
        if (Boolean.TRUE.equals(driver.getIsDeleted())) {
            throw notLinked();
        }
        if (user.getCompanyId() == null || !user.getCompanyId().equals(driver.getCompanyId())) {
            throw notLinked();
        }
        return driver;
    }

    @Transactional(readOnly = true)
    public void assertAuthorized(AppUser user, Driver driver, Vehicle vehicle) {
        if (driver == null || vehicle == null || vehicle.getId() == null || user == null) {
            throw notAuthorized();
        }
        if (Boolean.TRUE.equals(vehicle.getIsDeleted()) || Boolean.TRUE.equals(driver.getIsDeleted())) {
            throw notAuthorized();
        }
        if (user.getCompanyId() == null
                || !user.getCompanyId().equals(driver.getCompanyId())
                || !user.getCompanyId().equals(vehicle.getCompanyId())) {
            throw notAuthorized();
        }
        long matches = assignmentRepository.countActiveAuthorization(
                user.getId(), user.getCompanyId(), vehicle.getId());
        if (matches < 1) {
            throw notAuthorized();
        }
    }

    @Transactional(readOnly = true)
    public List<Long> authorizedVehicleIds(AppUser user) {
        Driver driver = requireLinkedDriver(user);
        return assignmentRepository.findActivelyAssignedVehicles(user.getCompanyId(), driver.getId()).stream()
                .map(Vehicle::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorizedVehicleResponse> authorizedVehicles(AppUser user) {
        Driver driver = requireLinkedDriver(user);
        return assignmentRepository.findActivelyAssignedVehicles(user.getCompanyId(), driver.getId()).stream()
                .map(this::toVehicle)
                .toList();
    }

    public boolean userHasRole(AppUser user, String roleCode) {
        if (user == null || user.getRoles() == null || roleCode == null) {
            return false;
        }
        return user.getRoles().stream().map(AppRole::getCode).anyMatch(roleCode::equals);
    }

    private AuthorizedVehicleResponse toVehicle(Vehicle vehicle) {
        AuthorizedVehicleResponse dto = new AuthorizedVehicleResponse();
        dto.setId(vehicle.getId());
        dto.setCode(vehicle.getCode());
        dto.setName(vehicle.getName());
        dto.setCompanyId(vehicle.getCompanyId());
        dto.setBranchId(vehicle.getBranchId());
        return dto;
    }

    private BusinessValidationException notLinked() {
        return new BusinessValidationException(
                "Driver Not Linked",
                "DRIVER_NOT_LINKED",
                "DRIVER_NOT_LINKED: This login is not linked to an operational driver.",
                "Ask an administrator to link your user to a driver record.");
    }

    private BusinessValidationException notAuthorized() {
        return new BusinessValidationException(
                "Vehicle Not Authorized",
                "DRIVER_VEHICLE_NOT_AUTHORIZED",
                "DRIVER_VEHICLE_NOT_AUTHORIZED: This driver is not assigned to the selected vehicle.",
                "Select a vehicle on your active assignment.");
    }
}
