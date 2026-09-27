package com.transport.erp.repository;

import com.transport.erp.model.SupplierPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    @Query("""
            SELECT p FROM SupplierPayment p WHERE p.companyId = :companyId AND p.isDeleted = false
              AND (:branchId IS NULL OR p.branchId = :branchId)
              AND (:supplierId IS NULL OR p.supplier.id = :supplierId)
              AND p.paymentDate BETWEEN :fromDate AND :toDate
            ORDER BY p.paymentDate DESC, p.id DESC
            """)
    Page<SupplierPayment> search(@Param("companyId") Long companyId, @Param("branchId") Long branchId, @Param("supplierId") Long supplierId,
                                 @Param("fromDate") LocalDate from, @Param("toDate") LocalDate to, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SupplierPayment p WHERE p.id = :id AND p.isDeleted = false")
    Optional<SupplierPayment> findByIdForUpdate(@Param("id") Long id);
}
