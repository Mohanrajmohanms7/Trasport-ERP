package com.transport.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "work_orders")
public class WorkOrder extends BaseEntity {

    @Column(name = "work_order_number", nullable = false, length = 50)
    private String workOrderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, length = 20)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_rule_id")
    private MaintenanceRule maintenanceRule;

    @Column(name = "maintenance_type", nullable = false, length = 50)
    private String maintenanceType;

    @Column(name = "trigger_mode", length = 20)
    private String triggerMode;

    @Column(name = "due_status_at_creation", length = 20)
    private String dueStatusAtCreation;

    @Column(name = "due_km", precision = 12, scale = 2)
    private BigDecimal dueKm;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "baseline_last_service_km", precision = 12, scale = 2)
    private BigDecimal baselineLastServiceKm;

    @Column(name = "baseline_last_service_date")
    private LocalDate baselineLastServiceDate;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Column(name = "odometer_at_open", precision = 12, scale = 2)
    private BigDecimal odometerAtOpen;

    @Column(name = "odometer_at_complete", precision = 12, scale = 2)
    private BigDecimal odometerAtComplete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private AppUser assignedUser;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "estimated_cost", precision = 14, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 14, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "attachment_path", columnDefinition = "TEXT")
    private String attachmentPath;

    @Column(name = "completed_by", length = 50)
    private String completedBy;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;
}
