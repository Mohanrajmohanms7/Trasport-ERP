-- Flyway Migration Script: V6__driver_management.sql
-- Creates database schemas for driver documents, daily attendance log, and monthly salary configuration parameters

-- 1. Driver Documents Table
CREATE TABLE driver_documents (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    doc_type VARCHAR(50) NOT NULL, -- AADHAAR, PAN, VISION_CERT, LICENSE_SCAN
    doc_number VARCHAR(100) NOT NULL,
    file_path TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_driver_doc_driver ON driver_documents(driver_id);

-- 2. Daily Attendance Log Table
CREATE TABLE driver_attendance (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- PRESENT, ABSENT, LEAVE, HALF_DAY
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_driver_attendance_driver ON driver_attendance(driver_id);

-- 3. Monthly Salary Configuration Parameters Table
CREATE TABLE driver_salaries (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    basic_salary NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    overtime_rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    advance_taken NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_driver_salary_driver ON driver_salaries(driver_id);
