-- Flyway Migration Script: V11__fuel_management.sql
-- Creates database schemas for fuel entries and fuel request approval workflow tables

-- 1. Fuel Entries Table
CREATE TABLE fuel_entries (
    id BIGSERIAL PRIMARY KEY,
    fuel_entry_number VARCHAR(100) NOT NULL UNIQUE,
    fuel_date DATE NOT NULL,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    driver_id BIGINT NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    trip_id BIGINT REFERENCES trips(id) ON DELETE SET NULL,
    fuel_station VARCHAR(150) NOT NULL,
    fuel_quantity NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    rate_per_litre NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    total_amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'CASH' NOT NULL, -- CASH, UPI, BANK, CREDIT
    invoice_number VARCHAR(100),
    current_odometer NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    previous_odometer NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    remarks TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_fuel_entry_vehicle ON fuel_entries(vehicle_id);
CREATE INDEX idx_fuel_entry_driver ON fuel_entries(driver_id);
CREATE INDEX idx_fuel_entry_trip ON fuel_entries(trip_id);

-- 2. Fuel Requests Table
CREATE TABLE fuel_requests (
    id BIGSERIAL PRIMARY KEY,
    request_number VARCHAR(100) NOT NULL UNIQUE,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    requested_quantity NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    requested_amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL, -- PENDING, APPROVED, REJECTED
    requested_by VARCHAR(100),
    approved_by VARCHAR(100),
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_fuel_request_trip ON fuel_requests(trip_id);
