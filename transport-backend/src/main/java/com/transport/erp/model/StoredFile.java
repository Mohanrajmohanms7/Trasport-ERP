package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** An uploaded file kept in PostgreSQL, stamped with the uploader's company/branch for access checks. */
@Getter
@Setter
@Entity
@Table(name = "stored_files")
public class StoredFile extends BaseEntity {

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @JsonIgnore
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false, columnDefinition = "bytea")
    private byte[] content;
}
