package com.transport.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "cin_number", length = 21)
    private String cinNumber;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String website;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String country;

    @Column(length = 105)
    private String pincode;

    @Column(columnDefinition = "TEXT")
    private String logo;

    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature;

    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(length = 50)
    private String storage = "10 GB";

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Column(name = "subscription_renewal_date")
    private LocalDate subscriptionRenewalDate;

    @Column(name = "subscription_status", length = 30)
    private String subscriptionStatus = "ACTIVE";

    @Column(name = "max_users")
    private Integer maxUsers = 5;

    @Column(name = "max_vehicles")
    private Integer maxVehicles = 5;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subscription_plan_id")
    private SaaSPlan subscriptionPlan;
}
