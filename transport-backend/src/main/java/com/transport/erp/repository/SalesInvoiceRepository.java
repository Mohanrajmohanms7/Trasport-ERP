package com.transport.erp.repository;

import com.transport.erp.model.SalesInvoice;
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
}
