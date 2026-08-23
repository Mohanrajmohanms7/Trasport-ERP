package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "saas_system_settings")
public class SaaSSystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_name", nullable = false, unique = true, length = 100)
    private String keyName;

    @Column(name = "value_data", nullable = false, columnDefinition = "TEXT")
    private String valueData;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_by", length = 50, nullable = false)
    private String updatedBy = "SYSTEM";

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
