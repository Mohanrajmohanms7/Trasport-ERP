-- Flyway Migration Script: V17__gps_ai_mobility.sql
-- Creates database schemas for GPS Tracking logs and AI Predictions tables

-- 1. GPS Tracking Logs Table
CREATE TABLE gps_trackings (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    latitude NUMERIC(10, 8) NOT NULL,
    longitude NUMERIC(11, 8) NOT NULL,
    speed NUMERIC(5, 2) DEFAULT 0.00 NOT NULL,
    ping_time TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_gps_vehicle ON gps_trackings(vehicle_id);
CREATE INDEX idx_gps_ping_time ON gps_trackings(ping_time);

-- 2. AI Predictions Table
CREATE TABLE ai_predictions (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(100) NOT NULL, -- MAINTENANCE, FUEL, TRIP_DELAY
    prediction_text TEXT NOT NULL,
    probability NUMERIC(5, 2) DEFAULT 0.00 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
