package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (createdBy == null) {
            createdBy = "SYSTEM";
        }
        if (status == null) {
            status = "ACTIVE";
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
