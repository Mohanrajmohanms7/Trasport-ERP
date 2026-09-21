package com.transport.erp.repository;

import com.transport.erp.model.VehicleMaintenanceBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleMaintenanceBaselineRepository extends JpaRepository<VehicleMaintenanceBaseline, Long> {

    Optional<VehicleMaintenanceBaseline> findByRule_IdAndVehicle_IdAndIsDeletedFalse(Long ruleId, Long vehicleId);

    @Query("""
            SELECT b FROM VehicleMaintenanceBaseline b
            JOIN FETCH b.rule
            JOIN FETCH b.vehicle
            WHERE b.companyId = :companyId AND b.isDeleted = false AND b.rule.id IN :ruleIds
            """)
    List<VehicleMaintenanceBaseline> findByCompanyIdAndIsDeletedFalseAndRule_IdIn(
            @Param("companyId") Long companyId,
            @Param("ruleIds") Collection<Long> ruleIds);
}
