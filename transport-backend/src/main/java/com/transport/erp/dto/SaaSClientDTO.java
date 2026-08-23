package com.transport.erp.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SaaSClientDTO {
    private Long id;
    private String name; // Company Name
    private String code; // Company Code
    private String ownerName;
    private String businessType;
    private String phone;
    private String email;
    private String gstNumber;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String logo;
    private String website;
    private String status;
    private String planName;
    private int licenseCount;
    private String storage;
    private LocalDateTime createdDate;
    private LocalDate expiryDate;
    private Long subscriptionPlanId;
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private LocalDate subscriptionRenewalDate;
    private String subscriptionStatus;
    private Integer maxUsers;
    private Integer maxVehicles;
}
