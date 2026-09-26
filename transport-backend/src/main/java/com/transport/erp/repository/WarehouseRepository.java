package com.transport.erp.repository;

import com.transport.erp.model.Warehouse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    @Query(value = """
            SELECT w.id FROM Warehouse w
            WHERE w.companyId = :companyId
              AND w.isDeleted = false
              AND (:branchId IS NULL OR w.branchId = :branchId)
              AND (:status IS NULL OR w.status = :status)
              AND (:code IS NULL OR :code = '' OR LOWER(w.code) LIKE LOWER(CONCAT('%', CAST(:code AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(w) FROM Warehouse w
            WHERE w.companyId = :companyId
              AND w.isDeleted = false
              AND (:branchId IS NULL OR w.branchId = :branchId)
              AND (:status IS NULL OR w.status = :status)
              AND (:code IS NULL OR :code = '' OR LOWER(w.code) LIKE LOWER(CONCAT('%', CAST(:code AS string), '%')))
            """)
    Page<Long> searchIds(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("status") String status,
            @Param("code") String code,
            Pageable pageable);

    @Query("""
            SELECT w FROM Warehouse w
            JOIN FETCH w.branch
            WHERE w.id IN :ids
            """)
    List<Warehouse> findDetailsByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT w FROM Warehouse w
            JOIN FETCH w.branch
            WHERE w.id = :id AND w.isDeleted = false
            """)
    Optional<Warehouse> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Warehouse w WHERE w.id = :id AND w.isDeleted = false")
    Optional<Warehouse> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT COUNT(s) FROM WarehouseStock s
            WHERE s.warehouse.id = :warehouseId
              AND s.isDeleted = false
              AND s.availableQuantity > 0
            """)
    long countPositiveStock(@Param("warehouseId") Long warehouseId);

    List<Warehouse> findByCompanyIdAndIsDeletedFalseOrderByCodeAsc(Long companyId);
}
