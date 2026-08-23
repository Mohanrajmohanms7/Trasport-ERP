-- Flyway Migration Script: V5__vehicle_management.sql
-- Creates database schema for fleet documents, workshop services history, and driver vehicle assignments log

-- 1. Fleet Documents Table
CREATE TABLE vehicle_documents (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    doc_type VARCHAR(50) NOT NULL, -- INSURANCE, PERMIT, FITNESS, PUC, ROAD_TAX
    doc_number VARCHAR(100) NOT NULL,
    expiry_date DATE NOT NULL,
    file_path TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vehicle_doc_vehicle ON vehicle_documents(vehicle_id);

-- 2. Workshop Services History Table
CREATE TABLE vehicle_services (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    service_type VARCHAR(50) NOT NULL, -- OIL_CHANGE, ENGINE_SERVICE, TYRE_CHANGE, BRAKE_SERVICE
    service_date DATE NOT NULL,
    next_service_date DATE,
    workshop VARCHAR(150),
    cost NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    remarks TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vehicle_service_vehicle ON vehicle_services(vehicle_id);

-- 3. Driver Vehicle Assignments Log
CREATE TABLE vehicle_driver_assignments (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    driver_id BIGINT NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    assignment_date DATE NOT NULL,
    removal_date DATE,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vehicle_assignment_vehicle ON vehicle_driver_assignments(vehicle_id);
CREATE INDEX idx_vehicle_assignment_driver ON vehicle_driver_assignments(driver_id);
