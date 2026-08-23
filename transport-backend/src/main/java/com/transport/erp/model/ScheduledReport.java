package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "scheduled_reports")
public class ScheduledReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "report_template_id", nullable = false)
    private ReportTemplate reportTemplate;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "recipient_email", nullable = false, length = 200)
    private String recipientEmail;

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE
}
