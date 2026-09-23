-- Phase 7: link a login account to an operational driver, and store maintenance requests.
-- Existing driver rows stay unmapped. No name, phone, or branch matching.
-- Does not alter work orders, parts, labour, journal vouchers, service logs, or inventory.

ALTER TABLE drivers
    ADD COLUMN app_user_id BIGINT REFERENCES app_users (id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX uk_drivers_app_user
    ON drivers (app_user_id)
    WHERE app_user_id IS NOT NULL
      AND is_deleted = FALSE;

CREATE INDEX idx_drivers_app_user
    ON drivers (app_user_id)
    WHERE app_user_id IS NOT NULL;

CREATE TABLE maintenance_requests (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    request_number VARCHAR(50) NOT NULL,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE RESTRICT,
    requested_by_user_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE RESTRICT,
    driver_id BIGINT REFERENCES drivers (id) ON DELETE RESTRICT,
    priority VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reported_odometer_km NUMERIC(12, 2),
    reviewed_by VARCHAR(50),
    reviewed_at TIMESTAMP,
    review_remarks TEXT,
    approved_by VARCHAR(50),
    approved_at TIMESTAMP,
    work_order_id BIGINT REFERENCES work_orders (id) ON DELETE RESTRICT,
    cancelled_by VARCHAR(50),
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT chk_maintenance_requests_status
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'APPROVED', 'CONVERTED', 'CANCELLED')),
    CONSTRAINT chk_maintenance_requests_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_maintenance_requests_odometer
        CHECK (reported_odometer_km IS NULL OR reported_odometer_km >= 0)
);

CREATE UNIQUE INDEX uk_maintenance_requests_company_number
    ON maintenance_requests (company_id, request_number);

CREATE UNIQUE INDEX uk_maintenance_requests_work_order
    ON maintenance_requests (work_order_id)
    WHERE work_order_id IS NOT NULL
      AND is_deleted = FALSE;

CREATE INDEX idx_maintenance_requests_company_status_requested
    ON maintenance_requests (company_id, status, requested_at DESC);

CREATE INDEX idx_maintenance_requests_company_vehicle
    ON maintenance_requests (company_id, vehicle_id, requested_at DESC);
