package com.transport.erp.repository;

import com.transport.erp.model.SaaSPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SaaSPlanRepository extends JpaRepository<SaaSPlan, Long> {
    Optional<SaaSPlan> findByCodeAndIsDeletedFalse(String code);
    Page<SaaSPlan> findByIsDeletedFalse(Pageable pageable);
    Page<SaaSPlan> findByIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
}
