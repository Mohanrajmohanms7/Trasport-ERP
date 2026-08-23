package com.transport.erp.repository;

import com.transport.erp.model.AppRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, Long> {
    Optional<AppRole> findByCodeAndIsDeletedFalse(String code);

    Optional<AppRole> findByCodeAndCompanyIdAndIsDeletedFalse(String code, Long companyId);

    java.util.List<AppRole> findAllByCodeAndIsDeletedFalse(String code);
    Page<AppRole> findByIsDeletedFalse(Pageable pageable);
}
