package com.transport.erp.repository;

import com.transport.erp.model.CustomerReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Repository
public interface CustomerReceiptRepository extends JpaRepository<CustomerReceipt, Long> {
    Optional<CustomerReceipt> findByReceiptNumberAndIsDeletedFalse(String receiptNumber);

    Page<CustomerReceipt> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    long countByCustomerIdAndIsDeletedFalse(Long customerId);

    @Query("""
            SELECT COALESCE(SUM(r.amountReceived), 0) FROM CustomerReceipt r
            WHERE r.companyId = :companyId AND r.isDeleted = false
            """)
    BigDecimal sumAmountReceivedByCompany(@Param("companyId") Long companyId);

    @Query("""
            SELECT COALESCE(SUM(r.amountReceived), 0) FROM CustomerReceipt r
            WHERE r.companyId = :companyId AND r.isDeleted = false
              AND r.receiptDate BETWEEN :from AND :to
            """)
    BigDecimal sumAmountReceivedByCompanyAndDateRange(@Param("companyId") Long companyId,
                                                      @Param("from") LocalDate from,
                                                      @Param("to") LocalDate to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM CustomerReceipt r
        WHERE r.id = :id
          AND r.isDeleted = false
    """)
    Optional<CustomerReceipt> findAndLockById(@Param("id") Long id);

    @Query("""
        SELECT r FROM CustomerReceipt r
        WHERE r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND (:status IS NULL OR r.status = :status)
          AND (:paymentMethod IS NULL OR r.paymentMethod = :paymentMethod)
          AND (:customerId IS NULL OR r.customer.id = :customerId)
          AND (:fromDate IS NULL OR r.receiptDate >= :fromDate)
          AND (:toDate IS NULL OR r.receiptDate <= :toDate)
          AND (:search IS NULL OR 
               LOWER(r.receiptNumber) LIKE :search OR 
               LOWER(r.referenceNumber) LIKE :search OR 
               LOWER(r.customer.name) LIKE :search
              )
          AND r.isDeleted = false
    """)
    Page<CustomerReceipt> findReceiptsWithFilters(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("status") String status,
            @Param("paymentMethod") String paymentMethod,
            @Param("customerId") Long customerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT r FROM CustomerReceipt r
        WHERE r.customer.id = :customerId
          AND r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND r.isDeleted = false
        ORDER BY r.receiptDate DESC, r.id DESC
    """)
    List<CustomerReceipt> findCustomerHistory(
            @Param("customerId") Long customerId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT r.customer.id, r.customer.name, r.customer.code,
               COALESCE(SUM(r.amountReceived), 0)
        FROM CustomerReceipt r
        WHERE r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND r.status = 'APPROVED'
          AND r.isDeleted = false
        GROUP BY r.customer.id, r.customer.name, r.customer.code
    """)
    List<Object[]> getCustomerTotalReceived(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT r.status, COUNT(r), COALESCE(SUM(r.amountReceived), 0)
        FROM CustomerReceipt r
        WHERE r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND (CAST(:fromDate AS LocalDate) IS NULL OR r.receiptDate >= :fromDate)
          AND (CAST(:toDate AS LocalDate) IS NULL OR r.receiptDate <= :toDate)
          AND r.isDeleted = false
        GROUP BY r.status
    """)
    List<Object[]> getReceiptSummaryStats(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT r FROM CustomerReceipt r
        WHERE r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND r.status = 'APPROVED'
          AND r.isDeleted = false
        ORDER BY r.receiptDate DESC, r.id DESC
    """)
    List<CustomerReceipt> findRecentApprovedReceipts(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            Pageable pageable);

    @Query("""
        SELECT r.customer.id, r.customer.name, COUNT(r.id), COALESCE(SUM(r.amountReceived), 0),
               COALESCE(SUM(r.advanceAmount), 0), MAX(r.receiptDate)
        FROM CustomerReceipt r
        WHERE r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND r.status = 'APPROVED'
          AND r.isDeleted = false
        GROUP BY r.customer.id, r.customer.name
        ORDER BY r.customer.name ASC
    """)
    Page<Object[]> getCustomerPaymentStatsPaginated(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            Pageable pageable);

    @Query("""
        SELECT r.customer.id, COALESCE(SUM(r.amountReceived), 0)
        FROM CustomerReceipt r
        WHERE r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND r.status = 'CANCELLED'
          AND r.isDeleted = false
        GROUP BY r.customer.id
    """)
    List<Object[]> getCustomerCancelledStats(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT r.customer.id, COALESCE(SUM(r.advanceAmount), 0), MAX(r.receiptDate)
        FROM CustomerReceipt r
        WHERE r.customer.id IN :customerIds
          AND r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND r.status = 'APPROVED'
          AND r.isDeleted = false
        GROUP BY r.customer.id
    """)
    List<Object[]> getCustomerAdvanceAndLastPayment(
            @Param("customerIds") List<Long> customerIds,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT r FROM CustomerReceipt r
        WHERE r.companyId = :companyId
          AND (:branchId IS NULL OR r.branchId = :branchId)
          AND r.isDeleted = false
    """)
    Page<CustomerReceipt> findReceiptsForReconciliation(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            Pageable pageable);
}


