package com.transport.erp.repository;

import com.transport.erp.model.SaaSBackup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaaSBackupRepository extends JpaRepository<SaaSBackup, Long> {
    Page<SaaSBackup> findAllByOrderByBackupDateDesc(Pageable pageable);
}
