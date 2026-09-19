package com.transport.erp.repository;

import com.transport.erp.model.VehicleOdometerReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleOdometerReadingRepository extends JpaRepository<VehicleOdometerReading, Long> {

    List<VehicleOdometerReading> findTop20ByVehicle_IdAndIsDeletedFalseOrderByReadingAtDescIdDesc(Long vehicleId);
}
