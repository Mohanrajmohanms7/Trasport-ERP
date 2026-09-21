-- Flyway Migration Script: V53__maintenance_rules_and_baselines.sql
-- Phase 2: Company-level maintenance rules and per-vehicle baselines.
-- Additive. No backfill. Due status is calculated at read time, not stored.

CREATE TABLE maintenance_rules (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    maintenance_type VARCHAR(50) NOT NULL,
    trigger_mode VARCHAR(20) NOT NULL,
    interval_km NUMERIC(12, 2),
    interval_days INT,
    due_soon_km NUMERIC(12, 2),
    due_soon_days INT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_maintenance_rules_trigger_mode
        CHECK (trigger_mode IN ('KM', 'DAYS', 'KM_OR_DAYS')),
    CONSTRAINT chk_maintenance_rules_interval_km
        CHECK (interval_km IS NULL OR interval_km >= 0),
    CONSTRAINT chk_maintenance_rules_interval_days
        CHECK (interval_days IS NULL OR interval_days >= 0),
    CONSTRAINT chk_maintenance_rules_due_soon_km
        CHECK (due_soon_km IS NULL OR due_soon_km >= 0),
    CONSTRAINT chk_maintenance_rules_due_soon_days
        CHECK (due_soon_days IS NULL OR due_soon_days >= 0)
);

CREATE UNIQUE INDEX uk_maintenance_rules_company_type_active
    ON maintenance_rules (company_id, maintenance_type)
    WHERE is_deleted = FALSE AND status = 'ACTIVE';

CREATE INDEX idx_maintenance_rules_company_status
    ON maintenance_rules (company_id, status);

CREATE INDEX idx_maintenance_rules_company_type
    ON maintenance_rules (company_id, maintenance_type);

CREATE TABLE vehicle_maintenance_baselines (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    rule_id BIGINT NOT NULL REFERENCES maintenance_rules (id),
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id),
    last_service_km NUMERIC(12, 2),
    last_service_date DATE,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_vehicle_maintenance_baselines_km
        CHECK (last_service_km IS NULL OR last_service_km >= 0)
);

CREATE UNIQUE INDEX uk_vehicle_maintenance_baselines_rule_vehicle
    ON vehicle_maintenance_baselines (rule_id, vehicle_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_vehicle_maintenance_baselines_company_vehicle
    ON vehicle_maintenance_baselines (company_id, vehicle_id);

CREATE INDEX idx_vehicle_maintenance_baselines_company_rule
    ON vehicle_maintenance_baselines (company_id, rule_id);
