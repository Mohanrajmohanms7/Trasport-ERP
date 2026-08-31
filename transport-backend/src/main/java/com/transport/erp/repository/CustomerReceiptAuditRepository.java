package com.transport.erp.repository;

import com.transport.erp.model.CustomerReceiptAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerReceiptAuditRepository extends JpaRepository<CustomerReceiptAudit, Long> {

    @Query("""
        SELECT a FROM CustomerReceiptAudit a
        WHERE a.receiptId = :receiptId
          AND a.companyId = :companyId
          AND (:branchId IS NULL OR a.branchId = :branchId)
          AND a.isDeleted = false
        ORDER BY a.eventTime ASC, a.id ASC
    """)
    List<CustomerReceiptAudit> findAuditTrail(
            @Param("receiptId") Long receiptId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);
}
