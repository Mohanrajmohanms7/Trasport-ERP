package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Maker-checker: when the company setting REQUIRE_SEPARATE_APPROVER is "true", the person who created a
 * document cannot approve it. Off by default so single-person offices keep working.
 */
@Service
public class ApprovalPolicyService {

    public static final String SETTING_KEY = "REQUIRE_SEPARATE_APPROVER";

    @Autowired
    private AppSettingService settingService;

    public boolean isEnabled(Long companyId) {
        return settingService.getByKey(SETTING_KEY, companyId)
                .map(s -> s.getValueData() != null && "true".equalsIgnoreCase(s.getValueData().trim()))
                .orElse(false);
    }

    public void assertDifferentApprover(Long companyId, String createdBy, String approver, String documentLabel) {
        if (createdBy == null || approver == null || !isEnabled(companyId)) return;
        if (createdBy.equalsIgnoreCase(approver)) {
            throw new BusinessValidationException(
                    "Second Person Must Approve",
                    "APPROVER_SAME_AS_CREATOR",
                    documentLabel + " was created by " + createdBy + ". Your company requires a different person to approve it.",
                    "Ask another authorised user to approve, or turn off " + SETTING_KEY + " in company settings.");
        }
    }
}
