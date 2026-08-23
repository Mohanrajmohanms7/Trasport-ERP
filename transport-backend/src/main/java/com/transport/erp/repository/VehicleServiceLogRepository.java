package com.transport.erp.repository;

import com.transport.erp.model.VehicleServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleServiceLogRepository extends JpaRepository<VehicleServiceLog, Long> {
    List<VehicleServiceLog> findByVehicleIdAndIsDeletedFalse(Long vehicleId);
}
