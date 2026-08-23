package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "saas_plans")
public class SaaSPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "billing_period", nullable = false, length = 50)
    private String billingPeriod = "MONTHLY";

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers = 5;

    @Column(name = "max_vehicles", nullable = false)
    private Integer maxVehicles = 5;

    @Column(name = "max_invoices", nullable = false)
    private Integer maxInvoices = 50;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy = "SYSTEM";

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Version
    private Integer version;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (createdBy == null) {
            createdBy = "SYSTEM";
        }
        if (isDeleted == null) {
            isDeleted = false;
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
