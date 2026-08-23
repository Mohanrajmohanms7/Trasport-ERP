package com.transport.erp.repository;

import com.transport.erp.model.VehicleDriverAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleDriverAssignmentRepository extends JpaRepository<VehicleDriverAssignment, Long> {
    List<VehicleDriverAssignment> findByVehicleIdAndIsDeletedFalse(Long vehicleId);
}
