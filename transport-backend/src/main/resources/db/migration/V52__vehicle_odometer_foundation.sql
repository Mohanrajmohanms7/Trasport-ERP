-- Flyway Migration Script: V52__vehicle_odometer_foundation.sql
-- Phase 1: Authoritative vehicle odometer KM. Additive and nullable.
-- Existing vehicles remain current_odometer_km = NULL (no fuel backfill).

ALTER TABLE vehicles
    ADD COLUMN current_odometer_km NUMERIC(12, 2) NULL,
    ADD COLUMN odometer_updated_at TIMESTAMP NULL;

CREATE TABLE vehicle_odometer_readings (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
    reading_km NUMERIC(12, 2) NOT NULL,
    reading_at TIMESTAMP NOT NULL,
    source VARCHAR(50) NOT NULL,
    reason TEXT,
    fuel_entry_id BIGINT NULL,
    work_order_id BIGINT NULL,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_vehicle_odometer_readings_vehicle
    ON vehicle_odometer_readings(vehicle_id);

CREATE INDEX idx_vehicle_odometer_readings_company_vehicle_at
    ON vehicle_odometer_readings(company_id, vehicle_id, reading_at DESC);

CREATE INDEX idx_vehicle_odometer_readings_source
    ON vehicle_odometer_readings(company_id, source);
