-- Flyway Migration Script: V8__material_quarry_management.sql
-- Alters queries tables to support compliance metadata, and builds loading locations and pricing tables

-- 1. Alter quarries
ALTER TABLE quarries ADD COLUMN owner_name VARCHAR(150);
ALTER TABLE quarries ADD COLUMN contact_number VARCHAR(20);
ALTER TABLE quarries ADD COLUMN gst_number VARCHAR(20);
ALTER TABLE quarries ADD COLUMN license_number VARCHAR(50);
ALTER TABLE quarries ADD COLUMN latitude NUMERIC(10, 8);
ALTER TABLE quarries ADD COLUMN longitude NUMERIC(11, 8);
ALTER TABLE quarries ADD COLUMN working_hours VARCHAR(100);

-- 2. Loading Locations Table
CREATE TABLE loading_locations (
    id BIGSERIAL PRIMARY KEY,
    location_code VARCHAR(50) NOT NULL UNIQUE,
    loading_point VARCHAR(150) NOT NULL,
    loading_charges NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    latitude NUMERIC(10, 8),
    longitude NUMERIC(11, 8),
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Material Pricing Table
CREATE TABLE material_prices (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    material_rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    transport_rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    royalty_rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    loading_charge NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    effective_date DATE NOT NULL,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_mat_price_material ON material_prices(material_id);
