package com.transport.erp.repository;

import com.transport.erp.model.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    Page<LoginHistory> findByUsername(String username, Pageable pageable);
    Page<LoginHistory> findByStatus(String status, Pageable pageable);
    Page<LoginHistory> findByStatusAndLogoutTimeIsNull(String status, Pageable pageable);
    Page<LoginHistory> findByLogoutTimeIsNotNull(Pageable pageable);
}
