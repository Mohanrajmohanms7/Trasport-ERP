package com.transport.erp.repository;

import com.transport.erp.model.SalesInvoice;
import com.transport.erp.dto.CustomerOutstandingSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.List;


@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {
    Optional<SalesInvoice> findByInvoiceNumberAndIsDeletedFalse(String invoiceNumber);

    Page<SalesInvoice> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<SalesInvoice> findByCompanyIdAndIsDeletedFalseAndStatus(Long companyId, String status, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(i.netAmount), 0) FROM SalesInvoice i
            WHERE i.companyId = :companyId AND i.isDeleted = false
              AND i.invoiceDate BETWEEN :from AND :to
              AND i.status IN :statuses
            """)
    BigDecimal sumNetAmountByCompanyAndDateRange(@Param("companyId") Long companyId,
                                                 @Param("from") LocalDate from,
                                                 @Param("to") LocalDate to,
                                                 @Param("statuses") Collection<String> statuses);

    @Query("""
            SELECT COALESCE(SUM(i.netAmount), 0) FROM SalesInvoice i
            WHERE i.companyId = :companyId AND i.isDeleted = false
              AND i.status IN :statuses
            """)
    BigDecimal sumNetAmountByCompanyAndStatuses(@Param("companyId") Long companyId,
                                                @Param("statuses") Collection<String> statuses);

    @Query("""
            SELECT i FROM SalesInvoice i JOIN i.details d
            WHERE d.trip.id = :tripId AND i.isDeleted = false AND i.status != 'CANCELLED'
            """)
    List<SalesInvoice> findInvoicesByTripId(@Param("tripId") Long tripId);

    @Query("""
            SELECT i FROM SalesInvoice i
            WHERE i.customer.id = :customerId
              AND i.companyId = :companyId
              AND (:branchId IS NULL OR i.branchId = :branchId)
              AND i.isDeleted = false
              AND i.status = 'APPROVED'
              AND i.paymentStatus != 'PAID'
            """)
    List<SalesInvoice> findOutstandingInvoices(
            @Param("customerId") Long customerId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM SalesInvoice i WHERE i.id = :id AND i.isDeleted = false")
    Optional<SalesInvoice> findAndLockById(@Param("id") Long id);

    @Query("""
        SELECT i FROM SalesInvoice i
        WHERE i.customer.id = :customerId
          AND i.companyId = :companyId
          AND (:branchId IS NULL OR i.branchId = :branchId)
          AND i.isDeleted = false
        ORDER BY i.invoiceDate DESC, i.id DESC
    """)
    Page<SalesInvoice> findByCustomerIdAndCompanyIdAndBranchId(
            @Param("customerId") Long customerId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(i.netAmount - i.paidAmount), 0) FROM SalesInvoice i
        WHERE i.customer.id = :customerId
          AND i.companyId = :companyId
          AND (:branchId IS NULL OR i.branchId = :branchId)
          AND i.status = 'APPROVED'
          AND i.isDeleted = false
    """)
    BigDecimal sumOutstandingByCustomer(
            @Param("customerId") Long customerId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT i.status, i.paymentStatus, COUNT(i), COALESCE(SUM(i.netAmount), 0), COALESCE(SUM(i.paidAmount), 0)
        FROM SalesInvoice i
        WHERE i.companyId = :companyId
          AND (:branchId IS NULL OR i.branchId = :branchId)
          AND i.status = 'APPROVED'
          AND i.isDeleted = false
        GROUP BY i.status, i.paymentStatus
    """)
    List<Object[]> getInvoiceSummaryStats(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT COUNT(DISTINCT i.customer.id)
        FROM SalesInvoice i
        WHERE i.companyId = :companyId
          AND (:branchId IS NULL OR i.branchId = :branchId)
          AND i.isDeleted = false
    """)
    long countCustomersWithInvoices(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT new com.transport.erp.dto.CustomerOutstandingSummaryDTO(
            c.id, c.name, c.code,
            COALESCE(SUM(CASE WHEN i.status = 'APPROVED' THEN i.netAmount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN i.status = 'APPROVED' THEN i.paidAmount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN i.status = 'APPROVED' THEN (i.netAmount - i.paidAmount) ELSE 0 END), 0),
            0.0,
            COUNT(i.id),
            SUM(CASE WHEN i.status = 'APPROVED' AND i.paymentStatus = 'UNPAID' THEN 1 ELSE 0 END),
            SUM(CASE WHEN i.status = 'APPROVED' AND i.paymentStatus = 'PARTIALLY_PAID' THEN 1 ELSE 0 END),
            SUM(CASE WHEN i.status = 'APPROVED' AND i.paymentStatus = 'PAID' THEN 1 ELSE 0 END),
            null
        )
        FROM SalesInvoice i
        JOIN i.customer c
        WHERE i.companyId = :companyId
          AND (:branchId IS NULL OR i.branchId = :branchId)
          AND i.isDeleted = false
          AND (:searchPattern IS NULL OR LOWER(c.name) LIKE :searchPattern OR LOWER(c.code) LIKE :searchPattern)
        GROUP BY c.id, c.name, c.code
        ORDER BY c.name ASC
    """)
    Page<CustomerOutstandingSummaryDTO> getCustomerOutstandingSummaries(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("""
        SELECT i
        FROM SalesInvoice i
        WHERE i.companyId = :companyId
          AND (:branchId IS NULL OR i.branchId = :branchId)
          AND (:customerId IS NULL OR i.customer.id = :customerId)
          AND i.status = 'APPROVED'
          AND i.paymentStatus != 'PAID'
          AND i.isDeleted = false
          AND (:searchPattern IS NULL OR 
               LOWER(i.invoiceNumber) LIKE :searchPattern OR 
               LOWER(i.customer.name) LIKE :searchPattern
              )
        ORDER BY i.invoiceDate DESC, i.id DESC
    """)
    List<SalesInvoice> findOutstandingInvoicesForAging(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("customerId") Long customerId,
            @Param("searchPattern") String searchPattern);

    @Query("""
        SELECT i.customer.id, COALESCE(SUM(i.netAmount - i.paidAmount), 0)
        FROM SalesInvoice i
        WHERE i.companyId = :companyId
          AND (:branchId IS NULL OR i.branchId = :branchId)
          AND i.status = 'APPROVED'
          AND i.isDeleted = false
        GROUP BY i.customer.id
    """)
    List<Object[]> getCustomerOutstandingBalances(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);
}


