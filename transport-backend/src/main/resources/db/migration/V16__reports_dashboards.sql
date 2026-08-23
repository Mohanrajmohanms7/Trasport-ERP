-- Flyway Migration Script: V16__reports_dashboards.sql
-- Creates database schemas for Report Templates and Scheduled Reports tracking tables

-- 1. Report Templates Table
CREATE TABLE report_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(200) NOT NULL,
    report_type VARCHAR(100) NOT NULL, -- FLEET, REVENUE, EXPENSE, TRIP, FUEL
    columns_list TEXT NOT NULL, -- comma-separated columns list
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Scheduled Reports Table
CREATE TABLE scheduled_reports (
    id BIGSERIAL PRIMARY KEY,
    report_template_id BIGINT NOT NULL REFERENCES report_templates(id) ON DELETE CASCADE,
    cron_expression VARCHAR(100) NOT NULL,
    recipient_email VARCHAR(200) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL, -- ACTIVE, INACTIVE
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sched_report_template ON scheduled_reports(report_template_id);
