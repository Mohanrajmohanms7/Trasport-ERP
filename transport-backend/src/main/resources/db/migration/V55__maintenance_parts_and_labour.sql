-- Flyway Migration Script: V55__maintenance_parts_and_labour.sql
-- Phase 5: Spare-part catalog and work-order parts/labour lines.
-- Additive. Does not alter work_orders, inventory, baselines, service logs, or accounting.

CREATE TABLE spare_parts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    default_uom_id BIGINT NOT NULL REFERENCES uom_master (id) ON DELETE RESTRICT,
    default_rate NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_spare_parts_default_rate CHECK (default_rate >= 0),
    CONSTRAINT chk_spare_parts_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uk_spare_parts_company_code
    ON spare_parts (company_id, code)
    WHERE is_deleted = false;

CREATE INDEX idx_spare_parts_company
    ON spare_parts (company_id);

CREATE TABLE work_order_parts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    work_order_id BIGINT NOT NULL REFERENCES work_orders (id) ON DELETE RESTRICT,
    spare_part_id BIGINT NOT NULL REFERENCES spare_parts (id) ON DELETE RESTRICT,
    quantity NUMERIC(12, 3) NOT NULL,
    unit_rate NUMERIC(14, 2) NOT NULL,
    line_total NUMERIC(14, 2) NOT NULL,
    uom_id BIGINT NOT NULL REFERENCES uom_master (id) ON DELETE RESTRICT,
    notes TEXT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_work_order_parts_quantity CHECK (quantity > 0),
    CONSTRAINT chk_work_order_parts_unit_rate CHECK (unit_rate >= 0),
    CONSTRAINT chk_work_order_parts_line_total CHECK (line_total >= 0)
);

CREATE INDEX idx_work_order_parts_work_order
    ON work_order_parts (work_order_id);

CREATE TABLE work_order_labour (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    work_order_id BIGINT NOT NULL REFERENCES work_orders (id) ON DELETE RESTRICT,
    app_user_id BIGINT REFERENCES app_users (id) ON DELETE RESTRICT,
    hours NUMERIC(8, 2) NOT NULL,
    rate NUMERIC(14, 2) NOT NULL,
    line_total NUMERIC(14, 2) NOT NULL,
    work_date DATE,
    notes TEXT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_work_order_labour_hours CHECK (hours > 0),
    CONSTRAINT chk_work_order_labour_rate CHECK (rate >= 0),
    CONSTRAINT chk_work_order_labour_line_total CHECK (line_total >= 0)
);

CREATE INDEX idx_work_order_labour_work_order
    ON work_order_labour (work_order_id);
