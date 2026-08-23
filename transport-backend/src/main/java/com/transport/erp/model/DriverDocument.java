package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "driver_documents")
public class DriverDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    @JsonIgnore
    private Driver driver;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType; // AADHAAR, PAN, VISION_CERT, LICENSE_SCAN

    @Column(name = "doc_number", nullable = false, length = 100)
    private String docNumber;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;
}
