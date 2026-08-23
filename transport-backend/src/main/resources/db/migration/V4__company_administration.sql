-- Flyway Migration Script: V4__company_administration.sql
-- Alters companies/branches tables to include specific contact and registration parameters, and builds settings tables

-- 1. Alter Companies
ALTER TABLE companies ADD COLUMN gst_number VARCHAR(20);
ALTER TABLE companies ADD COLUMN pan_number VARCHAR(10);
ALTER TABLE companies ADD COLUMN cin_number VARCHAR(21);
ALTER TABLE companies ADD COLUMN phone VARCHAR(20);
ALTER TABLE companies ADD COLUMN email VARCHAR(100);
ALTER TABLE companies ADD COLUMN website VARCHAR(100);
ALTER TABLE companies ADD COLUMN address TEXT;
ALTER TABLE companies ADD COLUMN city VARCHAR(50);
ALTER TABLE companies ADD COLUMN state VARCHAR(50);
ALTER TABLE companies ADD COLUMN country VARCHAR(50);
ALTER TABLE companies ADD COLUMN pincode VARCHAR(10);
ALTER TABLE companies ADD COLUMN logo TEXT;
ALTER TABLE companies ADD COLUMN digital_signature TEXT;

-- 2. Alter Branches
ALTER TABLE branches ADD COLUMN gst_number VARCHAR(20);
ALTER TABLE branches ADD COLUMN manager VARCHAR(100);
ALTER TABLE branches ADD COLUMN phone VARCHAR(20);
ALTER TABLE branches ADD COLUMN email VARCHAR(100);
ALTER TABLE branches ADD COLUMN address TEXT;
ALTER TABLE branches ADD COLUMN latitude NUMERIC(10, 8);
ALTER TABLE branches ADD COLUMN longitude NUMERIC(11, 8);

-- 3. Financial Years Table
CREATE TABLE financial_years (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Application Settings Table (Key Value parameters for emails, preferences, theme settings, backup locations)
CREATE TABLE app_settings (
    id BIGSERIAL PRIMARY KEY,
    key_name VARCHAR(100) NOT NULL UNIQUE,
    value_data TEXT,
    company_id BIGINT,
    branch_id BIGINT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
