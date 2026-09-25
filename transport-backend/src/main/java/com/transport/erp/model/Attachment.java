package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A document attached to any business record (entityType + entityId). The file lives in stored_files. */
@Getter
@Setter
@Entity
@Table(name = "attachments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Attachment extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(length = 40)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Transient
    public String getFileUrl() {
        return "/api/v1/files/download/" + fileName;
    }
}
