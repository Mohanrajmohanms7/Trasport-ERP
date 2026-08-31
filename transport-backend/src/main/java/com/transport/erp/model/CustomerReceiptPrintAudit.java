package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "customer_receipt_print_audit")
public class CustomerReceiptPrintAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // PRINT, PDF_EXPORT, REPRINT

    @Column(name = "printed_by", nullable = false, length = 100)
    private String printedBy;

    @Column(name = "printed_at", nullable = false)
    private LocalDateTime printedAt = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String format; // PRINT, PDF

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "created_date")
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_date")
    private LocalDateTime updatedDate = LocalDateTime.now();

    @Column(name = "created_by", length = 100)
    private String createdBy = "system";

    @Column(name = "updated_by", length = 100)
    private String updatedBy = "system";

    @Version
    private Integer version = 0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
