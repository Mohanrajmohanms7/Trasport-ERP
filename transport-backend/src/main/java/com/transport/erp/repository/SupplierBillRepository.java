package com.transport.erp.repository;

import com.transport.erp.model.SupplierBill;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierBillRepository extends JpaRepository<SupplierBill, Long> {

    @Query("""
            SELECT b FROM SupplierBill b WHERE b.companyId = :companyId AND b.isDeleted = false
              AND (:branchId IS NULL OR b.branchId = :branchId)
              AND (:supplierId IS NULL OR b.supplier.id = :supplierId)
              AND (:status IS NULL OR b.status = :status)
              AND (:paymentStatus IS NULL OR b.paymentStatus = :paymentStatus)
              AND b.billDate BETWEEN :fromDate AND :toDate
            ORDER BY b.billDate DESC, b.id DESC
            """)
    Page<SupplierBill> search(@Param("companyId") Long companyId, @Param("branchId") Long branchId, @Param("supplierId") Long supplierId,
                              @Param("status") String status, @Param("paymentStatus") String paymentStatus,
                              @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to, Pageable pageable);

    /** Open approved bills of a supplier, oldest due first, locked for allocation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM SupplierBill b WHERE b.supplier.id = :supplierId AND b.companyId = :companyId AND b.isDeleted = false
              AND b.status = 'APPROVED' AND b.paidAmount < b.totalAmount ORDER BY b.dueDate ASC, b.id ASC
            """)
    List<SupplierBill> lockOpenBills(@Param("companyId") Long companyId, @Param("supplierId") Long supplierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM SupplierBill b WHERE b.id = :id AND b.isDeleted = false")
    Optional<SupplierBill> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b FROM SupplierBill b WHERE b.companyId = :companyId AND b.sourceType = :sourceType AND b.sourceId = :sourceId AND b.isDeleted = false AND b.status <> 'CANCELLED'")
    Optional<SupplierBill> findActiveBySource(@Param("companyId") Long companyId, @Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);
}
