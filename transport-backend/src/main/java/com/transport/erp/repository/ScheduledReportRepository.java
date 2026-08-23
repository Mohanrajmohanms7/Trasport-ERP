package com.transport.erp.repository;

import com.transport.erp.model.ScheduledReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, Long> {
    Page<ScheduledReport> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
}
