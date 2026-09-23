package com.transport.erp.repository;

import com.transport.erp.model.WorkOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @Query(value = """
            SELECT w.id FROM WorkOrder w
            WHERE w.companyId = :companyId
              AND w.isDeleted = false
              AND (:vehicleId IS NULL OR w.vehicle.id = :vehicleId)
              AND (:status IS NULL OR w.status = :status)
              AND (:source IS NULL OR w.source = :source)
              AND (:maintenanceType IS NULL OR w.maintenanceType = :maintenanceType)
              AND (:branchId IS NULL OR w.branchId IS NULL OR w.branchId = :branchId)
            """,
            countQuery = """
            SELECT COUNT(w) FROM WorkOrder w
            WHERE w.companyId = :companyId
              AND w.isDeleted = false
              AND (:vehicleId IS NULL OR w.vehicle.id = :vehicleId)
              AND (:status IS NULL OR w.status = :status)
              AND (:source IS NULL OR w.source = :source)
              AND (:maintenanceType IS NULL OR w.maintenanceType = :maintenanceType)
              AND (:branchId IS NULL OR w.branchId IS NULL OR w.branchId = :branchId)
            """)
    Page<Long> searchIds(
            @Param("companyId") Long companyId,
            @Param("vehicleId") Long vehicleId,
            @Param("status") String status,
            @Param("source") String source,
            @Param("maintenanceType") String maintenanceType,
            @Param("branchId") Long branchId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT w FROM WorkOrder w
            JOIN FETCH w.vehicle
            LEFT JOIN FETCH w.maintenanceRule
            LEFT JOIN FETCH w.supplier
            LEFT JOIN FETCH w.assignedUser
            WHERE w.id IN :ids
            """)
    List<WorkOrder> findDetailsByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT w FROM WorkOrder w
            JOIN FETCH w.vehicle
            LEFT JOIN FETCH w.maintenanceRule
            LEFT JOIN FETCH w.supplier
            LEFT JOIN FETCH w.assignedUser
            WHERE w.id = :id AND w.isDeleted = false
            """)
    Optional<WorkOrder> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w FROM WorkOrder w
            JOIN FETCH w.vehicle
            WHERE w.id = :id AND w.isDeleted = false
            """)
    Optional<WorkOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT w FROM WorkOrder w
            JOIN FETCH w.vehicle
            LEFT JOIN FETCH w.supplier
            WHERE w.companyId = :companyId
              AND w.vehicle.id = :vehicleId
              AND w.status = 'COMPLETED'
              AND w.isDeleted = false
            """)
    List<WorkOrder> findCompletedForVehicle(@Param("companyId") Long companyId, @Param("vehicleId") Long vehicleId);

    @Query("""
            SELECT COUNT(w) FROM WorkOrder w
            WHERE w.companyId = :companyId
              AND w.vehicle.id = :vehicleId
              AND w.maintenanceRule.id = :ruleId
              AND w.source = 'PREVENTIVE'
              AND w.status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED')
              AND w.isDeleted = false
              AND ((:baselineKm IS NULL AND w.baselineLastServiceKm IS NULL) OR w.baselineLastServiceKm = :baselineKm)
              AND ((:baselineDate IS NULL AND w.baselineLastServiceDate IS NULL) OR w.baselineLastServiceDate = :baselineDate)
            """)
    long countPreventiveCycle(
            @Param("companyId") Long companyId,
            @Param("vehicleId") Long vehicleId,
            @Param("ruleId") Long ruleId,
            @Param("baselineKm") BigDecimal baselineKm,
            @Param("baselineDate") LocalDate baselineDate);

    Optional<WorkOrder> findFirstByAttachmentPathContainingAndIsDeletedFalse(String attachmentPath);

    /**
     * Phase 8 maintenance state. OPEN, COMPLETED, CANCELLED, and deleted rows do not match.
     */
    @Query("""
            SELECT CASE WHEN COUNT(w.id) > 0 THEN true ELSE false END
            FROM WorkOrder w
            WHERE w.vehicle.id = :vehicleId
              AND w.status = 'IN_PROGRESS'
              AND w.isDeleted = false
            """)
    boolean existsInProgressForVehicle(@Param("vehicleId") Long vehicleId);

    @Query("""
            SELECT DISTINCT w.vehicle.id
            FROM WorkOrder w
            WHERE w.vehicle.id IN :vehicleIds
              AND w.status = 'IN_PROGRESS'
              AND w.isDeleted = false
            """)
    List<Long> findVehicleIdsUnderMaintenance(@Param("vehicleIds") Collection<Long> vehicleIds);
}
