package com.transport.erp.repository;

import com.transport.erp.model.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);
    Page<Material> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    Page<Material> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(Long companyId, String name, String code, Pageable pageable);
}
