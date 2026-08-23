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

@Repository
public interface CustomerReceiptRepository extends JpaRepository<CustomerReceipt, Long> {
    Optional<CustomerReceipt> findByReceiptNumberAndIsDeletedFalse(String receiptNumber);

    Page<CustomerReceipt> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

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
}
