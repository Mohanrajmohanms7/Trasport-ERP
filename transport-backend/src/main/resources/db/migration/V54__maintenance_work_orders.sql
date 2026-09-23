-- Flyway Migration Script: V54__maintenance_work_orders.sql
-- Phase 4: Maintenance work orders.
-- Additive. Does not alter odometer readings, baselines, service logs, or accounting.

CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'OPEN' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    work_order_number VARCHAR(50) NOT NULL,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE RESTRICT,
    source VARCHAR(20) NOT NULL,
    maintenance_rule_id BIGINT REFERENCES maintenance_rules (id) ON DELETE RESTRICT,
    maintenance_type VARCHAR(50) NOT NULL,
    trigger_mode VARCHAR(20),
    due_status_at_creation VARCHAR(20),
    due_km NUMERIC(12, 2),
    due_date DATE,
    baseline_last_service_km NUMERIC(12, 2),
    baseline_last_service_date DATE,
    priority VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    requested_date DATE,
    odometer_at_open NUMERIC(12, 2),
    odometer_at_complete NUMERIC(12, 2),
    supplier_id BIGINT REFERENCES suppliers (id) ON DELETE RESTRICT,
    assigned_user_id BIGINT REFERENCES app_users (id) ON DELETE RESTRICT,
    diagnosis TEXT,
    completion_notes TEXT,
    cancellation_reason TEXT,
    estimated_cost NUMERIC(14, 2),
    actual_cost NUMERIC(14, 2),
    attachment_path TEXT,
    completed_by VARCHAR(50),
    cancelled_by VARCHAR(50),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_work_orders_source
        CHECK (source IN ('PREVENTIVE', 'MANUAL')),
    CONSTRAINT chk_work_orders_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_work_orders_priority
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH')),
    CONSTRAINT chk_work_orders_source_rule
        CHECK (
            (source = 'PREVENTIVE' AND maintenance_rule_id IS NOT NULL)
            OR (source = 'MANUAL' AND maintenance_rule_id IS NULL)
        ),
    CONSTRAINT chk_work_orders_estimated_cost
        CHECK (estimated_cost IS NULL OR estimated_cost >= 0),
    CONSTRAINT chk_work_orders_actual_cost
        CHECK (actual_cost IS NULL OR actual_cost >= 0),
    CONSTRAINT chk_work_orders_due_km
        CHECK (due_km IS NULL OR due_km >= 0),
    CONSTRAINT chk_work_orders_baseline_km
        CHECK (baseline_last_service_km IS NULL OR baseline_last_service_km >= 0),
    CONSTRAINT chk_work_orders_odometer_open
        CHECK (odometer_at_open IS NULL OR odometer_at_open >= 0),
    CONSTRAINT chk_work_orders_odometer_complete
        CHECK (odometer_at_complete IS NULL OR odometer_at_complete >= 0)
);

CREATE UNIQUE INDEX uk_work_orders_company_number
    ON work_orders (company_id, work_order_number);

CREATE INDEX idx_work_orders_company_status_opened
    ON work_orders (company_id, status, opened_at DESC);

CREATE INDEX idx_work_orders_company_vehicle_opened
    ON work_orders (company_id, vehicle_id, opened_at DESC);

-- One non-cancelled preventive job per service cycle.
-- NULL baselines compare equal (PostgreSQL NULLS NOT DISTINCT).
CREATE UNIQUE INDEX uk_work_orders_preventive_cycle
    ON work_orders (
        company_id,
        vehicle_id,
        maintenance_rule_id,
        baseline_last_service_km,
        baseline_last_service_date
    )
    NULLS NOT DISTINCT
    WHERE source = 'PREVENTIVE'
      AND status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED')
      AND is_deleted = FALSE;
