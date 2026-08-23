package com.transport.erp.repository;

import com.transport.erp.model.Expense;
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
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findByExpenseNumberAndIsDeletedFalse(String expenseNumber);

    Page<Expense> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<Expense> findByCompanyIdAndIsDeletedFalseAndStatus(Long companyId, String status, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e
            WHERE e.companyId = :companyId AND e.isDeleted = false
              AND e.expenseDate BETWEEN :from AND :to
              AND e.status <> 'CANCELLED'
            """)
    BigDecimal sumTotalAmountByCompanyAndDateRange(@Param("companyId") Long companyId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);
}
