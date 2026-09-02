package com.transport.erp.repository;

import com.transport.erp.model.SalesInvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SalesInvoiceDetailRepository extends JpaRepository<SalesInvoiceDetail, Long> {
    List<SalesInvoiceDetail> findByInvoiceIdAndIsDeletedFalse(Long invoiceId);

    @Query("SELECT COUNT(sid) FROM SalesInvoiceDetail sid WHERE sid.material.id = :materialId AND sid.invoice.isDeleted = false")
    long countByMaterialIdAndInvoiceIsDeletedFalse(@Param("materialId") Long materialId);
}
