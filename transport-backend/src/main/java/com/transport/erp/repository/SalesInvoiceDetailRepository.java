package com.transport.erp.repository;

import com.transport.erp.model.SalesInvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SalesInvoiceDetailRepository extends JpaRepository<SalesInvoiceDetail, Long> {
    List<SalesInvoiceDetail> findByInvoiceIdAndIsDeletedFalse(Long invoiceId);
}
