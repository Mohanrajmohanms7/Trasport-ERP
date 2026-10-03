package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One step in a support ticket's history (created, status / priority / assignee change, resolved, reopened, closed). */
@Getter
@Setter
@Entity
@Table(name = "saas_support_ticket_events")
public class SaaSSupportTicketEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    /** Internal events (assignment) are not shown to the client. */
    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = false;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}
