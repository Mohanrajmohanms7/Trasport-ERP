package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "saas_backups")
public class SaaSBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "file_size", nullable = false, length = 50)
    private String fileSize;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "backup_date", nullable = false)
    private LocalDateTime backupDate = LocalDateTime.now();

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType = "MANUAL";

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy = "SYSTEM";

    @PrePersist
    protected void onCreate() {
        backupDate = LocalDateTime.now();
    }
}
