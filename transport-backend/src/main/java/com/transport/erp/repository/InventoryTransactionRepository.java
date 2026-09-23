package com.transport.erp.repository;

import com.transport.erp.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @Query(value = """
            SELECT t.id FROM InventoryTransaction t
            JOIN t.sparePart p
            WHERE t.companyId = :companyId
              AND t.isDeleted = false
              AND (:warehouseId IS NULL OR t.warehouse.id = :warehouseId)
              AND (:sparePartId IS NULL OR p.id = :sparePartId)
              AND (:branchId IS NULL OR t.branchId = :branchId)
              AND (:transactionType IS NULL OR t.transactionType = :transactionType)
            """,
            countQuery = """
            SELECT COUNT(t) FROM InventoryTransaction t
            JOIN t.sparePart p
            WHERE t.companyId = :companyId
              AND t.isDeleted = false
              AND (:warehouseId IS NULL OR t.warehouse.id = :warehouseId)
              AND (:sparePartId IS NULL OR p.id = :sparePartId)
              AND (:branchId IS NULL OR t.branchId = :branchId)
              AND (:transactionType IS NULL OR t.transactionType = :transactionType)
            """)
    Page<Long> searchIds(
            @Param("companyId") Long companyId,
            @Param("warehouseId") Long warehouseId,
            @Param("sparePartId") Long sparePartId,
            @Param("branchId") Long branchId,
            @Param("transactionType") String transactionType,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM InventoryTransaction t
            JOIN FETCH t.warehouse w
            JOIN FETCH w.branch
            JOIN FETCH t.sparePart p
            WHERE t.id IN :ids
            """)
    List<InventoryTransaction> findDetailsByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT t FROM InventoryTransaction t
            JOIN FETCH t.warehouse w
            JOIN FETCH w.branch
            JOIN FETCH t.sparePart p
            WHERE t.id = :id AND t.isDeleted = false
            """)
    Optional<InventoryTransaction> findDetailById(@Param("id") Long id);
}
