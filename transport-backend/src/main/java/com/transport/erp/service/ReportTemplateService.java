package com.transport.erp.service;

import com.transport.erp.model.ReportTemplate;
import com.transport.erp.repository.ReportTemplateRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportTemplateService {

    @Autowired
    private ReportTemplateRepository templateRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;


    public Page<ReportTemplate> getTemplates(Long companyId, Pageable pageable) {
        return templateRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public ReportTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .filter(t -> !t.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Report template not found: " + id));
    }

    @Transactional
    public ReportTemplate createTemplate(ReportTemplate template, String username) {
        template.setIsDeleted(false);
        template.setCreatedBy(username);
        template.setUpdatedBy(username);

        template.setCompanyId(tenantAccess.resolveCompanyId(template.getCompanyId()));
        template.setBranchId(tenantAccess.resolveBranchId(template.getBranchId()));

        if (template.getCode() == null) template.setCode("REP-" + System.currentTimeMillis());
        if (template.getName() == null) template.setName(template.getTemplateName());

        ReportTemplate saved = templateRepository.save(template);

        auditService.log(username, "REPORT_TEMPLATE_CREATED", "report_templates", saved.getId(), null,
                "Registered custom report template: " + saved.getTemplateName());

        return saved;
    }

    @Transactional
    public void deleteTemplate(Long id, String username) {
        ReportTemplate template = getTemplateById(id);
        template.setIsDeleted(true);
        template.setUpdatedBy(username);
        templateRepository.save(template);

        auditService.log(username, "REPORT_TEMPLATE_DELETED", "report_templates", template.getId(), null,
                "Soft deleted custom report template: " + template.getTemplateName());
    }
}
