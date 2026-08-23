-- Flyway Migration Script: V10__trip_planning_dispatch.sql
-- Creates database schemas for trip headers and dispatch details tables

-- 1. Trips Headers Table
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    trip_number VARCHAR(100) NOT NULL UNIQUE,
    trip_date DATE NOT NULL,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    vehicle_id BIGINT REFERENCES vehicles(id) ON DELETE SET NULL,
    driver_id BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    status VARCHAR(50) DEFAULT 'PLANNED' NOT NULL, -- PLANNED, DISPATCHED, COMPLETED, CANCELLED
    remarks TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_trip_booking ON trips(booking_id);
CREATE INDEX idx_trip_vehicle ON trips(vehicle_id);
CREATE INDEX idx_trip_driver ON trips(driver_id);

-- 2. Trip Details Table
CREATE TABLE trip_details (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    quantity NUMERIC(12, 2) NOT NULL,
    rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    loading_charges NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    royalty NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    dispatch_time TIMESTAMP,
    arrival_time TIMESTAMP,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_trip_detail_trip ON trip_details(trip_id);
CREATE INDEX idx_trip_detail_material ON trip_details(material_id);
