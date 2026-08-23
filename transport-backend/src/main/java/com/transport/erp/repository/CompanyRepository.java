package com.transport.erp.repository;

import com.transport.erp.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByCodeAndIsDeletedFalse(String code);
    Page<Company> findByIsDeletedFalse(Pageable pageable);
    Page<Company> findByIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
}
