package com.transport.erp.repository;

import com.transport.erp.model.ReportTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    Page<ReportTemplate> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
}
