package com.transport.erp.repository;

import com.transport.erp.model.SupplierPaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierPaymentAllocationRepository extends JpaRepository<SupplierPaymentAllocation, Long> {
    List<SupplierPaymentAllocation> findByPaymentIdAndStatusAndIsDeletedFalse(Long paymentId, String status);

    List<SupplierPaymentAllocation> findByBillIdAndStatusAndIsDeletedFalse(Long billId, String status);
}
