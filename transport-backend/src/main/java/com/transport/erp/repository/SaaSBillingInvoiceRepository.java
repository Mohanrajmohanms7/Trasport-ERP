package com.transport.erp.repository;

import com.transport.erp.model.SaaSBillingInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaaSBillingInvoiceRepository extends JpaRepository<SaaSBillingInvoice, Long> {
    Page<SaaSBillingInvoice> findByCompanyId(Long companyId, Pageable pageable);
    Page<SaaSBillingInvoice> findAllByOrderByInvoiceDateDesc(Pageable pageable);
}
