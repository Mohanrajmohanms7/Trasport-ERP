package com.transport.erp.repository;

import com.transport.erp.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);
    Page<Customer> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    long countByCompanyIdAndIsDeletedFalse(Long companyId);
    Page<Customer> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(Long companyId, String name, String code, Pageable pageable);
}
