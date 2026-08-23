package com.transport.erp.repository;

import com.transport.erp.model.ChartOfAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    Optional<ChartOfAccount> findByAccountCodeAndIsDeletedFalse(String accountCode);

    Optional<ChartOfAccount> findByCompanyIdAndAccountCodeAndIsDeletedFalse(Long companyId, String accountCode);

    Page<ChartOfAccount> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
}
