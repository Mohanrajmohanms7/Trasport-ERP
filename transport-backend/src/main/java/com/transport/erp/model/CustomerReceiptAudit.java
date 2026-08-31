package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_receipt_audit")
public class CustomerReceiptAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime = LocalDateTime.now();

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(length = 255)
    private String remarks;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    private Integer version = 0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
