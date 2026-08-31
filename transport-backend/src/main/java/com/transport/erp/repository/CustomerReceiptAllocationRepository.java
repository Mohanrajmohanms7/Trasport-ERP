package com.transport.erp.repository;

import com.transport.erp.model.CustomerReceiptAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerReceiptAllocationRepository extends JpaRepository<CustomerReceiptAllocation, Long> {

    @Query("""
        SELECT a FROM CustomerReceiptAllocation a
        WHERE a.receipt.id = :receiptId
          AND a.receipt.companyId = :companyId
          AND (:branchId IS NULL OR a.receipt.branchId = :branchId)
          AND a.isDeleted = false
    """)
    List<CustomerReceiptAllocation> findByReceiptIdAndCompanyIdAndBranchId(
            @Param("receiptId") Long receiptId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT a FROM CustomerReceiptAllocation a
        WHERE a.invoice.id = :invoiceId
          AND a.invoice.companyId = :companyId
          AND (:branchId IS NULL OR a.invoice.branchId = :branchId)
          AND a.isDeleted = false
    """)
    List<CustomerReceiptAllocation> findByInvoiceIdAndCompanyIdAndBranchId(
            @Param("invoiceId") Long invoiceId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("""
        SELECT a.receipt.id, COALESCE(SUM(a.allocatedAmount), 0), COUNT(a.id)
        FROM CustomerReceiptAllocation a
        WHERE a.receipt.id IN :receiptIds
          AND a.isDeleted = false
        GROUP BY a.receipt.id
    """)
    List<Object[]> sumAllocationsAndCountForReceipts(@Param("receiptIds") List<Long> receiptIds);

    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM CustomerReceiptAllocation a
        WHERE a.receipt.companyId = :companyId
          AND (:branchId IS NULL OR a.receipt.branchId = :branchId)
          AND (CAST(:fromDate AS LocalDate) IS NULL OR a.receipt.receiptDate >= :fromDate)
          AND (CAST(:toDate AS LocalDate) IS NULL OR a.receipt.receiptDate <= :toDate)
          AND a.isDeleted = false
          AND a.receipt.status = 'APPROVED'
          AND a.receipt.isDeleted = false
    """)
    java.math.BigDecimal sumAllocatedAmount(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate);
}

