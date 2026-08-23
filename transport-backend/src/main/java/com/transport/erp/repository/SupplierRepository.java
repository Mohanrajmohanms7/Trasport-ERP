package com.transport.erp.repository;

import com.transport.erp.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);
    Page<Supplier> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    Page<Supplier> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(Long companyId, String name, String code, Pageable pageable);
}
