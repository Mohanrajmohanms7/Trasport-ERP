package com.transport.erp.repository;

import com.transport.erp.model.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);
    Page<Branch> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    Page<Branch> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(Long companyId, String name, String code, Pageable pageable);
}
