package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.ScheduledReport;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.ScheduledReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports/schedule")
@CrossOrigin(origins = "*")
public class ScheduledReportController {

    @Autowired
    private ScheduledReportService scheduleService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<ScheduledReport>> getSchedules(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<ScheduledReport> data = scheduleService.getSchedules(targetCompanyId, pageable);
        return ApiResponse.success(data, "Scheduled reports triggers fetched successfully");
    }

    @PostMapping
    public ApiResponse<ScheduledReport> create(@RequestBody ScheduledReport schedule) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        schedule.setCompanyId(tenantAccess.resolveCompanyId(schedule.getCompanyId()));
        ScheduledReport created = scheduleService.createSchedule(schedule, activeUser);
        return ApiResponse.success(created, "Report cron trigger scheduled successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        ScheduledReport existing = scheduleService.getScheduleById(id);
        tenantAccess.assertCompanyAccess(existing.getCompanyId());
        scheduleService.deleteSchedule(id, activeUser);
        return ApiResponse.success(null, "Scheduled trigger deleted successfully");
    }
}
