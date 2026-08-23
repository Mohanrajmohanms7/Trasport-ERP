package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.ReportTemplate;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.ReportExportService;
import com.transport.erp.service.ReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@CrossOrigin(origins = "*")
public class ReportTemplateController {

    @Autowired
    private ReportTemplateService templateService;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private ReportExportService reportExportService;

    @GetMapping
    public ApiResponse<Page<ReportTemplate>> getTemplates(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<ReportTemplate> data = templateService.getTemplates(targetCompanyId, pageable);
        return ApiResponse.success(data, "Report templates fetched successfully");
    }

    @PostMapping
    public ApiResponse<ReportTemplate> create(@RequestBody ReportTemplate template) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        template.setCompanyId(tenantAccess.resolveCompanyId(template.getCompanyId()));
        ReportTemplate created = templateService.createTemplate(template, activeUser);
        return ApiResponse.success(created, "Report template registered successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        ReportTemplate existing = templateService.getTemplateById(id);
        tenantAccess.assertCompanyAccess(existing.getCompanyId());
        templateService.deleteTemplate(id, activeUser);
        return ApiResponse.success(null, "Report template deleted successfully");
    }

    @PostMapping("/export")
    public ApiResponse<Map<String, String>> exportReport(@RequestBody Map<String, String> exportRequest) {
        if (exportRequest == null || exportRequest.get("templateId") == null) {
            throw new IllegalArgumentException("templateId is required");
        }
        Long templateId = Long.valueOf(exportRequest.get("templateId"));
        return ApiResponse.success(
                reportExportService.exportTemplate(templateId),
                "Report exported successfully");
    }
}
