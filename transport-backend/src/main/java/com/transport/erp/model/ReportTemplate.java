package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "report_templates")
public class ReportTemplate extends BaseEntity {

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "report_type", nullable = false, length = 100)
    private String reportType; // FLEET, REVENUE, EXPENSE, TRIP, FUEL

    @Column(name = "columns_list", nullable = false, columnDefinition = "TEXT")
    private String columnsList;
}
