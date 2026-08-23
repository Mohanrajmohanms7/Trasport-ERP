package com.transport.erp.repository;

import com.transport.erp.model.FinancialYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FinancialYearRepository extends JpaRepository<FinancialYear, Long> {
    Optional<FinancialYear> findByCodeAndIsDeletedFalse(String code);

    Optional<FinancialYear> findByCodeAndCompanyIdAndIsDeletedFalse(String code, Long companyId);

    Page<FinancialYear> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
}
