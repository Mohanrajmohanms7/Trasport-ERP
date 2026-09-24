package com.transport.erp.repository;

import com.transport.erp.model.WarehouseStock;
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
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {

    @Query(value = """
            SELECT s.id FROM WarehouseStock s
            JOIN s.sparePart p
            WHERE s.companyId = :companyId
              AND s.isDeleted = false
              AND (:warehouseId IS NULL OR s.warehouse.id = :warehouseId)
              AND (:sparePartId IS NULL OR p.id = :sparePartId)
              AND (:branchId IS NULL OR s.branchId = :branchId)
              AND (:code IS NULL OR :code = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', CAST(:code AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(s) FROM WarehouseStock s
            JOIN s.sparePart p
            WHERE s.companyId = :companyId
              AND s.isDeleted = false
              AND (:warehouseId IS NULL OR s.warehouse.id = :warehouseId)
              AND (:sparePartId IS NULL OR p.id = :sparePartId)
              AND (:branchId IS NULL OR s.branchId = :branchId)
              AND (:code IS NULL OR :code = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', CAST(:code AS string), '%')))
            """)
    Page<Long> searchIds(
            @Param("companyId") Long companyId,
            @Param("warehouseId") Long warehouseId,
            @Param("sparePartId") Long sparePartId,
            @Param("branchId") Long branchId,
            @Param("code") String code,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT s FROM WarehouseStock s
            JOIN FETCH s.warehouse w
            JOIN FETCH w.branch
            JOIN FETCH s.sparePart p
            JOIN FETCH p.defaultUom
            WHERE s.id IN :ids
            """)
    List<WarehouseStock> findDetailsByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT s FROM WarehouseStock s
            JOIN FETCH s.warehouse w
            JOIN FETCH w.branch
            JOIN FETCH s.sparePart p
            JOIN FETCH p.defaultUom
            WHERE s.id = :id AND s.isDeleted = false
            """)
    Optional<WarehouseStock> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM WarehouseStock s
            WHERE s.warehouse.id = :warehouseId
              AND s.sparePart.id = :sparePartId
              AND s.isDeleted = false
            """)
    Optional<WarehouseStock> findActiveForUpdate(
            @Param("warehouseId") Long warehouseId,
            @Param("sparePartId") Long sparePartId);
}
