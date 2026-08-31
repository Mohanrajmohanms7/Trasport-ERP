package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.ScheduledReport;
import com.transport.erp.repository.ScheduledReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledReportService {

    @Autowired
    private ScheduledReportRepository scheduleRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private AuditService auditService;




    public Page<ScheduledReport> getSchedules(Long companyId, Pageable pageable) {
        return scheduleRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public ScheduledReport getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .filter(s -> !s.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Scheduled report not found: " + id));
    }

    @Transactional
    public ScheduledReport createSchedule(ScheduledReport schedule, String username) {
        schedule.setIsDeleted(false);
        schedule.setCreatedBy(username);
        schedule.setUpdatedBy(username);

        schedule.setCompanyId(tenantAccess.resolveCompanyId(schedule.getCompanyId()));
        schedule.setBranchId(tenantAccess.resolveBranchId(schedule.getBranchId()));

        if (schedule.getCode() == null) schedule.setCode("SCH-" + System.currentTimeMillis());
        if (schedule.getName() == null) schedule.setName("Scheduled Report Trigger");

        ScheduledReport saved = scheduleRepository.save(schedule);

        auditService.log(username, "REPORT_SCHEDULE_CREATED", "scheduled_reports", saved.getId(), null,
                "Registered scheduled trigger for report template ID: " + saved.getReportTemplate().getId());

        return saved;
    }

    @Transactional
    public void deleteSchedule(Long id, String username) {
        ScheduledReport schedule = getScheduleById(id);
        schedule.setIsDeleted(true);
        schedule.setUpdatedBy(username);
        scheduleRepository.save(schedule);

        auditService.log(username, "REPORT_SCHEDULE_DELETED", "scheduled_reports", schedule.getId(), null,
                "Soft deleted scheduled trigger ID: " + schedule.getId());
    }
}
