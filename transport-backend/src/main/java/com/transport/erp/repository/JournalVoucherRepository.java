package com.transport.erp.repository;

import com.transport.erp.model.JournalVoucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalVoucherRepository extends JpaRepository<JournalVoucher, Long> {
    Page<JournalVoucher> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    List<JournalVoucher> findByReferenceNumberAndIsDeletedFalse(String referenceNumber);

    @Query("SELECT COALESCE(SUM(jv.amount), 0) FROM JournalVoucher jv WHERE jv.debitAccount.id = :accountId AND jv.companyId = :companyId AND jv.voucherDate BETWEEN :startDate AND :endDate AND jv.isDeleted = false")
    BigDecimal sumDebitByAccountAndCompanyAndDateRange(@Param("accountId") Long accountId, @Param("companyId") Long companyId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(jv.amount), 0) FROM JournalVoucher jv WHERE jv.creditAccount.id = :accountId AND jv.companyId = :companyId AND jv.voucherDate BETWEEN :startDate AND :endDate AND jv.isDeleted = false")
    BigDecimal sumCreditByAccountAndCompanyAndDateRange(@Param("accountId") Long accountId, @Param("companyId") Long companyId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(jv.amount), 0) FROM JournalVoucher jv WHERE jv.debitAccount.id = :accountId AND jv.companyId = :companyId AND jv.voucherDate < :date AND jv.isDeleted = false")
    BigDecimal sumDebitByAccountAndCompanyBeforeDate(@Param("accountId") Long accountId, @Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(jv.amount), 0) FROM JournalVoucher jv WHERE jv.creditAccount.id = :accountId AND jv.companyId = :companyId AND jv.voucherDate < :date AND jv.isDeleted = false")
    BigDecimal sumCreditByAccountAndCompanyBeforeDate(@Param("accountId") Long accountId, @Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(jv.amount), 0) FROM JournalVoucher jv WHERE jv.debitAccount.id = :accountId AND jv.companyId = :companyId AND jv.voucherDate <= :date AND jv.isDeleted = false")
    BigDecimal sumDebitByAccountAndCompanyOnOrBeforeDate(@Param("accountId") Long accountId, @Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(jv.amount), 0) FROM JournalVoucher jv WHERE jv.creditAccount.id = :accountId AND jv.companyId = :companyId AND jv.voucherDate <= :date AND jv.isDeleted = false")
    BigDecimal sumCreditByAccountAndCompanyOnOrBeforeDate(@Param("accountId") Long accountId, @Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT jv FROM JournalVoucher jv WHERE (jv.debitAccount.id = :accountId OR jv.creditAccount.id = :accountId) AND jv.companyId = :companyId AND jv.voucherDate BETWEEN :startDate AND :endDate AND jv.isDeleted = false ORDER BY jv.voucherDate ASC, jv.id ASC")
    List<JournalVoucher> findGeneralLedgerEntries(@Param("accountId") Long accountId, @Param("companyId") Long companyId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
