package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleDriverAssignment;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.VehicleDriverAssignmentRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleDriverAssignmentService {

    @Autowired
    private VehicleDriverAssignmentRepository assignmentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private AuditService auditService;

    public List<VehicleDriverAssignment> getAssignmentsByVehicle(Long vehicleId) {
        parentAccess.requireVehicle(vehicleId);
        return assignmentRepository.findByVehicleIdAndIsDeletedFalse(vehicleId);
    }

    public Optional<VehicleDriverAssignment> getActiveAssignmentByVehicle(Long vehicleId) {
        parentAccess.requireVehicle(vehicleId);
        return assignmentRepository.findByVehicleIdAndRemovalDateIsNullAndIsDeletedFalse(vehicleId);
    }

    @Transactional
    public VehicleDriverAssignment assignDriver(Long vehicleId, Long driverId, String username) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new BusinessValidationException(
                        "Vehicle Not Found",
                        "VEHICLE_NOT_FOUND",
                        "Vehicle not found with ID: " + vehicleId,
                        "Verify the vehicle ID."
                ));

        Driver driver = driverRepository.findByIdForUpdate(driverId)
                .orElseThrow(() -> new BusinessValidationException(
                        "Driver Not Found",
                        "DRIVER_NOT_FOUND",
                        "Driver not found with ID: " + driverId,
                        "Verify the driver ID."
                ));

        tenantAccess.assertOwned(vehicle.getCompanyId());
        tenantAccess.assertOwned(driver.getCompanyId());

        if (!vehicle.getCompanyId().equals(driver.getCompanyId())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle belongs to Company ID %d, Driver belongs to Company ID %d.", vehicle.getCompanyId(), driver.getCompanyId()));
            throw new BusinessValidationException(
                    "Company Mismatch",
                    "VEHICLE_DRIVER_COMPANY_MISMATCH",
                    "Vehicle and Driver must belong to the same company.",
                    "Select a driver and vehicle within the same company.",
                    details
            );
        }

        if (vehicle.getBranchId() != null && driver.getBranchId() != null && !vehicle.getBranchId().equals(driver.getBranchId())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle belongs to Branch ID %d, Driver belongs to Branch ID %d.", vehicle.getBranchId(), driver.getBranchId()));
            throw new BusinessValidationException(
                    "Branch Mismatch",
                    "VEHICLE_DRIVER_BRANCH_MISMATCH",
                    "Vehicle and Driver must belong to the same branch context.",
                    "Select a driver and vehicle within compatible branch scope.",
                    details
            );
        }

        String vehStatus = vehicle.getStatus() != null ? vehicle.getStatus().toUpperCase() : "ACTIVE";
        if (!"ACTIVE".equals(vehStatus) && !"AVAILABLE".equals(vehStatus)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle '%s' current status is %s.", vehicle.getName(), vehicle.getStatus()));
            throw new BusinessValidationException(
                    "Vehicle Assignment Blocked",
                    "VEHICLE_ASSIGNMENT_BLOCKED",
                    String.format("Vehicle '%s' status is %s and cannot be assigned to a driver.", vehicle.getName(), vehicle.getStatus()),
                    "Vehicle must be in ACTIVE or AVAILABLE status.",
                    details
            );
        }

        String drvStatus = driver.getStatus() != null ? driver.getStatus().toUpperCase() : "ACTIVE";
        if (!"ACTIVE".equals(drvStatus) && !"AVAILABLE".equals(drvStatus)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Driver '%s' current status is %s.", driver.getName(), driver.getStatus()));
            throw new BusinessValidationException(
                    "Driver Assignment Blocked",
                    "DRIVER_ASSIGNMENT_BLOCKED",
                    String.format("Driver '%s' status is %s and cannot be assigned to a vehicle.", driver.getName(), driver.getStatus()),
                    "Driver must be in ACTIVE or AVAILABLE status.",
                    details
            );
        }

        Optional<VehicleDriverAssignment> exactPair = assignmentRepository
                .findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(vehicleId, driverId);
        if (exactPair.isPresent()) {
            return exactPair.get();
        }

        Optional<VehicleDriverAssignment> activeVeh = assignmentRepository.findActiveByVehicleIdForUpdate(vehicleId);
        if (activeVeh.isPresent()) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle '%s' is currently assigned to Driver '%s'.", vehicle.getName(), activeVeh.get().getDriver().getName()));
            throw new BusinessValidationException(
                    "Vehicle Already Assigned",
                    "VEHICLE_ALREADY_ASSIGNED",
                    String.format("Vehicle '%s' is already actively assigned to Driver '%s'.", vehicle.getName(), activeVeh.get().getDriver().getName()),
                    "Unassign the existing driver before assigning another driver.",
                    details
            );
        }

        Optional<VehicleDriverAssignment> activeDrv = assignmentRepository.findActiveByDriverIdForUpdate(driverId);
        if (activeDrv.isPresent()) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Driver '%s' is currently assigned to Vehicle '%s'.", driver.getName(), activeDrv.get().getVehicle().getName()));
            throw new BusinessValidationException(
                    "Driver Already Assigned",
                    "DRIVER_ALREADY_ASSIGNED",
                    String.format("Driver '%s' is already actively assigned to Vehicle '%s'.", driver.getName(), activeDrv.get().getVehicle().getName()),
                    "Unassign the existing vehicle before assigning another vehicle.",
                    details
            );
        }

        VehicleDriverAssignment assignment = new VehicleDriverAssignment();
        assignment.setVehicle(vehicle);
        assignment.setDriver(driver);
        assignment.setAssignmentDate(LocalDate.now());
        assignment.setRemovalDate(null);
        assignment.setStatus("ACTIVE");
        assignment.setCode("ASSIGN_" + vehicleId + "_" + driverId + "_" + System.currentTimeMillis() % 10000);
        assignment.setName("Driver Assignment - " + vehicle.getName() + " / " + driver.getName());
        assignment.setCompanyId(vehicle.getCompanyId());
        assignment.setBranchId(vehicle.getBranchId() != null ? vehicle.getBranchId() : driver.getBranchId());
        assignment.setIsDeleted(false);
        assignment.setCreatedBy(username);
        assignment.setUpdatedBy(username);

        VehicleDriverAssignment saved = assignmentRepository.save(assignment);

        auditService.log(username, "VEHICLE_DRIVER_ASSIGNED", "vehicle_driver_assignments", saved.getId(), null,
                "Assigned driver " + driver.getName() + " to vehicle " + vehicle.getName());

        return saved;
    }

    @Transactional
    public VehicleDriverAssignment unassignDriver(Long vehicleId, String username) {
        parentAccess.requireVehicle(vehicleId);

        VehicleDriverAssignment active = assignmentRepository.findActiveByVehicleIdForUpdate(vehicleId)
                .orElseThrow(() -> {
                    List<String> details = new ArrayList<>();
                    details.add("No active assignment found for vehicle ID " + vehicleId);
                    return new BusinessValidationException(
                            "Assignment Already Closed",
                            "ASSIGNMENT_ALREADY_CLOSED",
                            "Vehicle has no active driver assignment to unassign.",
                            "Assign a driver first before attempting unassignment.",
                            details
                    );
                });

        tenantAccess.assertOwned(active.getCompanyId());

        active.setRemovalDate(LocalDate.now());
        active.setStatus("CLOSED");
        active.setUpdatedBy(username);

        VehicleDriverAssignment saved = assignmentRepository.save(active);

        auditService.log(username, "VEHICLE_DRIVER_UNASSIGNED", "vehicle_driver_assignments", saved.getId(), null,
                "Unassigned driver from vehicle ID " + vehicleId);

        return saved;
    }
}

