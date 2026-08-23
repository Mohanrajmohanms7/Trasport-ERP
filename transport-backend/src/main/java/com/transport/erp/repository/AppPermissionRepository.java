package com.transport.erp.repository;

import com.transport.erp.model.AppPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AppPermissionRepository extends JpaRepository<AppPermission, Long> {
    Optional<AppPermission> findByCodeAndIsDeletedFalse(String code);
    Page<AppPermission> findByIsDeletedFalse(Pageable pageable);
}
