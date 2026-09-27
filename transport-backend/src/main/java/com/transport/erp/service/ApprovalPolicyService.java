package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Maker-checker: when the company setting REQUIRE_SEPARATE_APPROVER is "true", the person who created a
 * document cannot approve it. Off by default, and never applied while the company has only one active staff login.
 */
@Service
public class ApprovalPolicyService {

    public static final String SETTING_KEY = "REQUIRE_SEPARATE_APPROVER";

    @Autowired
    private AppSettingService settingService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /** Active, non-driver logins of the company (people who could approve). */
    long activeStaffUsers(Long companyId) {
        Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT u.id) FROM app_users u JOIN user_roles ur ON ur.user_id = u.id JOIN app_roles r ON r.id = ur.role_id "
                        + "WHERE u.company_id = ? AND u.is_deleted = false AND u.status = 'ACTIVE' AND r.code NOT IN ('DRIVER', 'VIEWER')",
                Long.class, companyId);
        return n == null ? 0 : n;
    }

    public boolean isEnabled(Long companyId) {
        return settingService.getByKey(SETTING_KEY, companyId)
                .map(s -> s.getValueData() != null && "true".equalsIgnoreCase(s.getValueData().trim()))
                .orElse(false);
    }

    public void assertDifferentApprover(Long companyId, String createdBy, String approver, String documentLabel) {
        if (createdBy == null || approver == null || !isEnabled(companyId)) return;
        // One-person company: nobody else can approve, so the rule cannot apply.
        if (activeStaffUsers(companyId) < 2) return;
        if (createdBy.equalsIgnoreCase(approver)) {
            throw new BusinessValidationException(
                    "Second Person Must Approve",
                    "APPROVER_SAME_AS_CREATOR",
                    documentLabel + " was created by " + createdBy + ". Your company requires a different person to approve it.",
                    "Ask another authorised user to approve, or turn off " + SETTING_KEY + " in company settings.");
        }
    }
}
