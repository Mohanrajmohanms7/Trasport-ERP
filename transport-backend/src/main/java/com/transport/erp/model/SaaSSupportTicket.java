package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "saas_support_tickets")
public class SaaSSupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 100)
    private String ticketNumber;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String priority = "MEDIUM";

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate = LocalDateTime.now();

    // Context captured when the issue was reported (V78)
    @Column(length = 80) private String module;
    @Column(length = 150) private String screen;
    @Column(name = "page_url", length = 500) private String pageUrl;
    @Column(name = "record_type", length = 50) private String recordType;
    @Column(name = "record_id") private Long recordId;
    @Column(name = "record_label", length = 150) private String recordLabel;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "app_version", length = 50) private String appVersion;

    // Handling
    @Column(name = "assigned_to", length = 100) private String assignedTo;
    @Column(name = "last_client_activity") private LocalDateTime lastClientActivity;
    @Column(name = "last_admin_activity") private LocalDateTime lastAdminActivity;
    @Column(name = "first_responded_at") private LocalDateTime firstRespondedAt;
    @Column(name = "first_response_due") private LocalDateTime firstResponseDue;
    @Column(name = "resolve_due") private LocalDateTime resolveDue;
    @Column(columnDefinition = "TEXT") private String resolution;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    @Column(name = "resolved_by", length = 100) private String resolvedBy;
    @Column(name = "closed_at") private LocalDateTime closedAt;
    @Column(name = "reopen_count", nullable = false) private Integer reopenCount = 0;
    @Version private Integer version = 0;

    /** Never serialised: replies include internal notes; screens get a filtered timeline from SupportTicketService. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SaaSSupportReply> replies = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
