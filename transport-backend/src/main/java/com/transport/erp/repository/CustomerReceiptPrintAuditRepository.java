package com.transport.erp.repository;

import com.transport.erp.model.CustomerReceiptPrintAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerReceiptPrintAuditRepository extends JpaRepository<CustomerReceiptPrintAudit, Long> {

    @Query("""
        SELECT a FROM CustomerReceiptPrintAudit a
        WHERE a.receiptId = :receiptId
          AND a.companyId = :companyId
          AND (:branchId IS NULL OR a.branchId = :branchId)
          AND a.isDeleted = false
        ORDER BY a.printedAt ASC
    """)
    List<CustomerReceiptPrintAudit> findByReceiptIdAndCompanyIdAndBranchIdOrderByPrintedAtAsc(
            @Param("receiptId") Long receiptId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId
    );
}
