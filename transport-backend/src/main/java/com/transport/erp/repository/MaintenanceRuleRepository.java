package com.transport.erp.repository;

import com.transport.erp.model.MaintenanceRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRuleRepository extends JpaRepository<MaintenanceRule, Long> {

    Page<MaintenanceRule> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Optional<MaintenanceRule> findByIdAndIsDeletedFalse(Long id);

    Optional<MaintenanceRule> findByCompanyIdAndMaintenanceTypeAndStatusAndIsDeletedFalse(
            Long companyId, String maintenanceType, String status);

    List<MaintenanceRule> findByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status);

    List<MaintenanceRule> findAllByCompanyIdAndMaintenanceTypeAndStatusAndIsDeletedFalse(
            Long companyId, String maintenanceType, String status);
}
