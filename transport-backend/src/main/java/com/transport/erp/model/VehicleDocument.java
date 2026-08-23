package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "vehicle_documents")
public class VehicleDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType; // INSURANCE, PERMIT, FITNESS, PUC, ROAD_TAX

    @Column(name = "doc_number", nullable = false, length = 100)
    private String docNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;
}
