package com.transport.erp.repository;

import com.transport.erp.model.FuelRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FuelRequestRepository extends JpaRepository<FuelRequest, Long> {
    Page<FuelRequest> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    Page<FuelRequest> findByCompanyIdAndIsDeletedFalseAndStatus(Long companyId, String status, Pageable pageable);
}
