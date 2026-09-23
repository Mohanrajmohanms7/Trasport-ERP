package com.transport.erp.service;

import com.transport.erp.repository.WorkOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Derived vehicle maintenance state. A vehicle is under maintenance only when
 * it has a non-deleted work order in IN_PROGRESS. Open, completed, cancelled,
 * and deleted work orders do not count, and neither do maintenance requests.
 */
@Service
public class VehicleMaintenanceStateService {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    public boolean isVehicleUnderMaintenance(Long vehicleId) {
        if (vehicleId == null) {
            return false;
        }
        return workOrderRepository.existsInProgressForVehicle(vehicleId);
    }

    public Set<Long> findVehiclesUnderMaintenance(Collection<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(workOrderRepository.findVehicleIdsUnderMaintenance(vehicleIds));
    }
}
