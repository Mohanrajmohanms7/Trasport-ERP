package com.transport.erp.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class ClientOnboardingResult {
    private Long companyId;
    private String companyName;
    private String companyCode;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private String adminUsername;
    private String temporaryPassword;
    private Long adminUserId;
    private String planName;
    private String planCode;
    private String subscriptionStatus;
    private String subscriptionStartDate;
    private String subscriptionEndDate;
    private String licenseKey;
    private String welcomeEmailStatus;
    private String welcomeEmailNote;
    private boolean clientReady;

    @Builder.Default
    private List<OnboardingStep> steps = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    public static class OnboardingStep {
        private int order;
        private String code;
        private String label;
        private String status; // DONE | SKIPPED | PENDING
        private String detail;
    }
}
