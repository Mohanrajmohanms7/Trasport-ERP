package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer_documents")
public class CustomerDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType; // GST_CERT, PAN_CARD, KYC, AGREEMENT

    @Column(name = "doc_number", nullable = false, length = 100)
    private String docNumber;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;
}
