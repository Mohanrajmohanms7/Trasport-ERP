package com.transport.erp.repository;

import com.transport.erp.model.VehicleDriverAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleDriverAssignmentRepository extends JpaRepository<VehicleDriverAssignment, Long> {

    List<VehicleDriverAssignment> findByVehicleIdAndIsDeletedFalse(Long vehicleId);

    Optional<VehicleDriverAssignment> findByVehicleIdAndRemovalDateIsNullAndIsDeletedFalse(Long vehicleId);

    Optional<VehicleDriverAssignment> findByDriverIdAndRemovalDateIsNullAndIsDeletedFalse(Long driverId);

    Optional<VehicleDriverAssignment> findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(Long vehicleId, Long driverId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM VehicleDriverAssignment a WHERE a.id = :id AND a.isDeleted = false")
    Optional<VehicleDriverAssignment> findAndLockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM VehicleDriverAssignment a WHERE a.vehicle.id = :vehicleId AND a.removalDate IS NULL AND a.isDeleted = false")
    Optional<VehicleDriverAssignment> findActiveByVehicleIdForUpdate(@Param("vehicleId") Long vehicleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM VehicleDriverAssignment a WHERE a.driver.id = :driverId AND a.removalDate IS NULL AND a.isDeleted = false")
    Optional<VehicleDriverAssignment> findActiveByDriverIdForUpdate(@Param("driverId") Long driverId);
}
