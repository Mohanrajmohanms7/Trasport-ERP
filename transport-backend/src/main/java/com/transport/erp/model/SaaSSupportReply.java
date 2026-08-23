package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "saas_support_replies")
public class SaaSSupportReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @JsonBackReference
    private SaaSSupportTicket ticket;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_admin_reply", nullable = false)
    private Boolean isAdminReply = false;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
