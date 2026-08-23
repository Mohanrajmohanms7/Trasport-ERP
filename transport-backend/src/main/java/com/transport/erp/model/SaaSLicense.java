package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "saas_licenses")
public class SaaSLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "license_key", nullable = false, unique = true, length = 255)
    private String licenseKey;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "activation_date", nullable = false)
    private LocalDate activationDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers = 5;

    @Column(name = "max_vehicles", nullable = false)
    private Integer maxVehicles = 5;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy = "SYSTEM";

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (createdBy == null) {
            createdBy = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        if (updatedBy == null) {
            updatedBy = "SYSTEM";
        }
    }
}
