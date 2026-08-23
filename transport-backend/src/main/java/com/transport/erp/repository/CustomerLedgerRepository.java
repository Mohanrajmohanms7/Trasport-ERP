package com.transport.erp.repository;

import com.transport.erp.model.CustomerLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {
    List<CustomerLedger> findByCustomerIdAndIsDeletedFalseOrderByIdAsc(Long customerId);
    Page<CustomerLedger> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
}
