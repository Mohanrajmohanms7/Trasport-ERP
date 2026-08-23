package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientOnboardingRequest {
    private String name;
    private String ownerName;
    private String businessType;
    private String phone;
    private String email;
    private String gstNumber;
    private String panNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String website;
    private String logo;
    private String storage;
    /** Optional; auto-generated when blank. */
    private String code;
    /** SaaS plan to assign. Falls back to TRIAL then BASIC. */
    private Long subscriptionPlanId;
    private String subscriptionStartDate;
    private String subscriptionEndDate;
    private Integer billingMonths;
    private Integer maxUsers;
    private Integer maxVehicles;
    /** When true (default), queue a future-ready welcome email notification. */
    private Boolean sendWelcomeEmail = true;
    
    private String adminUsername;
    private String adminPassword;
}
