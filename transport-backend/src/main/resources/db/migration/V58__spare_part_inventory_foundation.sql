-- Flyway Migration Script: V58__spare_part_inventory_foundation.sql
-- Phase 9: Warehouse/store, spare-part stock balance, and opening-balance transactions.
-- Quantity-only. Does not alter spare_parts, work_order_parts, accounting, or trip blocking.

CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL REFERENCES branches (id) ON DELETE RESTRICT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_warehouses_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uk_warehouses_company_branch_code
    ON warehouses (company_id, branch_id, code)
    WHERE is_deleted = false;

CREATE INDEX idx_warehouses_company_branch_deleted
    ON warehouses (company_id, branch_id, is_deleted);

CREATE TABLE warehouse_stock (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    spare_part_id BIGINT NOT NULL REFERENCES spare_parts (id) ON DELETE RESTRICT,
    available_quantity NUMERIC(12, 3) NOT NULL DEFAULT 0,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_warehouse_stock_qty CHECK (available_quantity >= 0)
);

CREATE UNIQUE INDEX uk_warehouse_stock_warehouse_part
    ON warehouse_stock (warehouse_id, spare_part_id)
    WHERE is_deleted = false;

CREATE INDEX idx_warehouse_stock_warehouse_part_deleted
    ON warehouse_stock (warehouse_id, spare_part_id, is_deleted);

CREATE INDEX idx_warehouse_stock_company_branch_deleted
    ON warehouse_stock (company_id, branch_id, is_deleted);

CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses (id) ON DELETE RESTRICT,
    spare_part_id BIGINT NOT NULL REFERENCES spare_parts (id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_inventory_tx_type CHECK (transaction_type IN ('OPENING_BALANCE')),
    CONSTRAINT chk_inventory_tx_qty CHECK (quantity > 0)
);

CREATE INDEX idx_inventory_tx_warehouse_part_created
    ON inventory_transactions (warehouse_id, spare_part_id, created_date);

CREATE INDEX idx_inventory_tx_company_branch_created
    ON inventory_transactions (company_id, branch_id, created_date);
