package com.transport.erp.repository;

import com.transport.erp.model.Quarry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface QuarryRepository extends JpaRepository<Quarry, Long> {
    Optional<Quarry> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);
    Page<Quarry> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    Page<Quarry> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(Long companyId, String name, String code, Pageable pageable);
}
