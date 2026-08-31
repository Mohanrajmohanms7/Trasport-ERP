package com.transport.erp.repository;

import com.transport.erp.model.JournalVoucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalVoucherRepository extends JpaRepository<JournalVoucher, Long> {
    Page<JournalVoucher> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    java.util.List<JournalVoucher> findByReferenceNumberAndIsDeletedFalse(String referenceNumber);
}

