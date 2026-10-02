package com.transport.erp.repository;

import com.transport.erp.model.FamilyExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FamilyExpenseRepository extends JpaRepository<FamilyExpense, Long> {

    @Query("""
            SELECT f FROM FamilyExpense f
            WHERE f.companyId = :companyId AND f.isDeleted = false
              AND f.expenseDate BETWEEN :from AND :to
              AND (:category IS NULL OR f.category = :category)
              AND (:mode IS NULL OR f.paymentMode = :mode)
            ORDER BY f.expenseDate DESC, f.id DESC
            """)
    Page<FamilyExpense> search(@Param("companyId") Long companyId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                               @Param("category") String category, @Param("mode") String mode, Pageable pageable);

    long countByCompanyIdAndCategoryAndIsDeletedFalse(Long companyId, String category);
}
