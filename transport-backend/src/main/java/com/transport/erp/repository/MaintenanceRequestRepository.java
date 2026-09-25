package com.transport.erp.repository;

import com.transport.erp.model.MaintenanceRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    @Query(value = """
            SELECT r.id FROM MaintenanceRequest r
            WHERE r.companyId = :companyId
              AND r.isDeleted = false
              AND (:vehicleId IS NULL OR r.vehicle.id = :vehicleId)
              AND (:status IS NULL OR r.status = :status)
              AND (:priority IS NULL OR r.priority = :priority)
              AND (:requestedById IS NULL OR r.requestedBy.id = :requestedById)
              AND r.requestedAt >= :fromAt
              AND r.requestedAt < :toAt
              AND (:branchId IS NULL OR r.branchId IS NULL OR r.branchId = :branchId)
            """,
            countQuery = """
            SELECT COUNT(r) FROM MaintenanceRequest r
            WHERE r.companyId = :companyId
              AND r.isDeleted = false
              AND (:vehicleId IS NULL OR r.vehicle.id = :vehicleId)
              AND (:status IS NULL OR r.status = :status)
              AND (:priority IS NULL OR r.priority = :priority)
              AND (:requestedById IS NULL OR r.requestedBy.id = :requestedById)
              AND r.requestedAt >= :fromAt
              AND r.requestedAt < :toAt
              AND (:branchId IS NULL OR r.branchId IS NULL OR r.branchId = :branchId)
            """)
    Page<Long> searchIds(
            @Param("companyId") Long companyId,
            @Param("vehicleId") Long vehicleId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("requestedById") Long requestedById,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            @Param("branchId") Long branchId,
            Pageable pageable);

    @Query(value = """
            SELECT r.id FROM MaintenanceRequest r
            WHERE r.companyId = :companyId
              AND r.isDeleted = false
              AND r.vehicle.id IN :vehicleIds
              AND (:vehicleId IS NULL OR r.vehicle.id = :vehicleId)
              AND (:status IS NULL OR r.status = :status)
              AND (:priority IS NULL OR r.priority = :priority)
              AND (:requestedById IS NULL OR r.requestedBy.id = :requestedById)
              AND r.requestedAt >= :fromAt
              AND r.requestedAt < :toAt
            """,
            countQuery = """
            SELECT COUNT(r) FROM MaintenanceRequest r
            WHERE r.companyId = :companyId
              AND r.isDeleted = false
              AND r.vehicle.id IN :vehicleIds
              AND (:vehicleId IS NULL OR r.vehicle.id = :vehicleId)
              AND (:status IS NULL OR r.status = :status)
              AND (:priority IS NULL OR r.priority = :priority)
              AND (:requestedById IS NULL OR r.requestedBy.id = :requestedById)
              AND r.requestedAt >= :fromAt
              AND r.requestedAt < :toAt
            """)
    Page<Long> searchIdsForVehicles(
            @Param("companyId") Long companyId,
            @Param("vehicleIds") Collection<Long> vehicleIds,
            @Param("vehicleId") Long vehicleId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("requestedById") Long requestedById,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM MaintenanceRequest r
            JOIN FETCH r.vehicle
            JOIN FETCH r.requestedBy
            LEFT JOIN FETCH r.driver
            LEFT JOIN FETCH r.workOrder
            WHERE r.id IN :ids
            """)
    List<MaintenanceRequest> findDetailsByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT r FROM MaintenanceRequest r
            JOIN FETCH r.vehicle
            JOIN FETCH r.requestedBy
            LEFT JOIN FETCH r.driver
            LEFT JOIN FETCH r.workOrder
            WHERE r.id = :id AND r.isDeleted = false
            """)
    Optional<MaintenanceRequest> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM MaintenanceRequest r
            JOIN FETCH r.vehicle
            JOIN FETCH r.requestedBy
            WHERE r.id = :id AND r.isDeleted = false
            """)
    Optional<MaintenanceRequest> findByIdForUpdate(@Param("id") Long id);

    java.util.List<com.transport.erp.model.MaintenanceRequest> findByCompanyIdAndIsDeletedFalseOrderByIdDesc(Long companyId);
}
