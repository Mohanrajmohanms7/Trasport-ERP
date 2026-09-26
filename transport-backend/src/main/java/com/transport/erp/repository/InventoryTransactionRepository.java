package com.transport.erp.repository;

import com.transport.erp.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
              AND (:workOrderId IS NULL OR t.workOrder.id = :workOrderId)
              AND (:reference = '' OR LOWER(t.code) LIKE LOWER(CONCAT('%', CAST(:reference AS string), '%')))
              AND (:createdBy = '' OR t.createdBy = :createdBy)
              AND (:supplierId IS NULL OR t.supplier.id = :supplierId)
              AND t.createdDate >= :fromDate
              AND t.createdDate <= :toDate
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
              AND (:workOrderId IS NULL OR t.workOrder.id = :workOrderId)
              AND (:reference = '' OR LOWER(t.code) LIKE LOWER(CONCAT('%', CAST(:reference AS string), '%')))
              AND (:createdBy = '' OR t.createdBy = :createdBy)
              AND (:supplierId IS NULL OR t.supplier.id = :supplierId)
              AND t.createdDate >= :fromDate
              AND t.createdDate <= :toDate
            """)
    Page<Long> searchIds(
            @Param("companyId") Long companyId,
            @Param("warehouseId") Long warehouseId,
            @Param("sparePartId") Long sparePartId,
            @Param("branchId") Long branchId,
            @Param("transactionType") String transactionType,
            @Param("workOrderId") Long workOrderId,
            @Param("reference") String reference,
            @Param("createdBy") String createdBy,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("supplierId") Long supplierId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM InventoryTransaction t
            JOIN FETCH t.warehouse w
            JOIN FETCH w.branch
            JOIN FETCH t.sparePart p
            LEFT JOIN FETCH t.workOrder
            LEFT JOIN FETCH t.supplier
            WHERE t.id IN :ids
            """)
    List<InventoryTransaction> findDetailsByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT t FROM InventoryTransaction t
            JOIN FETCH t.warehouse w
            JOIN FETCH w.branch
            JOIN FETCH t.sparePart p
            LEFT JOIN FETCH t.workOrder
            LEFT JOIN FETCH t.supplier
            WHERE t.id = :id AND t.isDeleted = false
            """)
    Optional<InventoryTransaction> findDetailById(@Param("id") Long id);

    @Query("""
            SELECT COALESCE(SUM(t.quantity), 0) FROM InventoryTransaction t
            WHERE t.workOrderPart.id = :workOrderPartId
              AND t.warehouse.id = :warehouseId
              AND t.transactionType = :transactionType
              AND t.isDeleted = false
            """)
    BigDecimal sumQuantity(
            @Param("warehouseId") Long warehouseId,
            @Param("workOrderPartId") Long workOrderPartId,
            @Param("transactionType") String transactionType);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.transactionType = 'ISSUE' THEN -t.quantity ELSE t.quantity END), 0)
            FROM InventoryTransaction t
            WHERE t.warehouse.id = :warehouseId
              AND t.sparePart.id = :sparePartId
              AND t.isDeleted = false
            """)
    BigDecimal sumSignedQuantity(
            @Param("warehouseId") Long warehouseId,
            @Param("sparePartId") Long sparePartId);

    @Query("""
            SELECT COUNT(t) FROM InventoryTransaction t
            WHERE t.companyId = :companyId
              AND t.transactionType = 'RECEIPT'
              AND t.isDeleted = false
              AND LOWER(t.externalReference) = LOWER(:externalReference)
            """)
    long countReceiptExternalReference(
            @Param("companyId") Long companyId,
            @Param("externalReference") String externalReference);

    java.util.List<com.transport.erp.model.InventoryTransaction> findByCompanyIdAndIsDeletedFalseOrderByIdDesc(Long companyId);

    @Query("""
            SELECT COALESCE(SUM(t.quantity * COALESCE(t.unitRate, 0)), 0), COALESCE(SUM(t.quantity), 0)
            FROM InventoryTransaction t
            WHERE t.workOrderPart.id = :lineId AND t.transactionType = :type AND t.isDeleted = false
            """)
    java.util.List<Object[]> valueAndQuantityForLine(@Param("lineId") Long lineId, @Param("type") String type);

    @Query("""
            SELECT COALESCE(SUM(t.quantity * COALESCE(t.unitRate, 0)), 0)
            FROM InventoryTransaction t
            WHERE t.workOrder.id = :workOrderId AND t.transactionType = :type AND t.isDeleted = false
            """)
    java.math.BigDecimal valueForWorkOrder(@Param("workOrderId") Long workOrderId, @Param("type") String type);
}
