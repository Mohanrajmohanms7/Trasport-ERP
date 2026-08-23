package com.transport.erp.service;

import com.transport.erp.model.Driver;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleDriverAssignment;
import com.transport.erp.repository.VehicleDriverAssignmentRepository;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VehicleDriverAssignmentService {

    @Autowired
    private VehicleDriverAssignmentRepository assignmentRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    public List<VehicleDriverAssignment> getAssignmentsByVehicle(Long vehicleId) {
        parentAccess.requireVehicle(vehicleId);
        return assignmentRepository.findByVehicleIdAndIsDeletedFalse(vehicleId);
    }

    @Transactional
    public VehicleDriverAssignment assignDriver(Long vehicleId, Long driverId) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        Driver driver = parentAccess.requireDriver(driverId);

        assignmentRepository.findByVehicleIdAndIsDeletedFalse(vehicleId).stream()
                .filter(a -> a.getRemovalDate() == null)
                .forEach(a -> {
                    a.setRemovalDate(LocalDate.now());
                    assignmentRepository.save(a);
                });

        VehicleDriverAssignment assignment = new VehicleDriverAssignment();
        assignment.setVehicle(vehicle);
        assignment.setDriver(driver);
        assignment.setAssignmentDate(LocalDate.now());
        assignment.setCode("ASSIGN_" + vehicleId + "_" + driverId);
        assignment.setName("Driver Assignment");
        assignment.setCompanyId(vehicle.getCompanyId());
        assignment.setBranchId(vehicle.getBranchId());
        assignment.setIsDeleted(false);

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void unassignDriver(Long vehicleId) {
        parentAccess.requireVehicle(vehicleId);
        assignmentRepository.findByVehicleIdAndIsDeletedFalse(vehicleId).stream()
                .filter(a -> a.getRemovalDate() == null)
                .forEach(a -> {
                    a.setRemovalDate(LocalDate.now());
                    assignmentRepository.save(a);
                });
    }
}
