-- Flyway Migration Script: V2__create_masters.sql
-- Creates database schema for Master Data tables using BIGSERIAL for 64-bit integer IDs matching Java Long

-- 1. Companies Table
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 2. Branches Table
CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    branch_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT uq_branch_code UNIQUE (company_id, code)
);

-- 3. App Roles Table
CREATE TABLE app_roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 4. App Permissions Table
CREATE TABLE app_permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 5. Role-Permissions Mapping Table
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES app_roles(id),
    permission_id BIGINT NOT NULL REFERENCES app_permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- 6. App Users Table
CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 7. User-Roles Mapping Table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES app_users(id),
    role_id BIGINT NOT NULL REFERENCES app_roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- 8. Common Lookup Values Table
-- Unified table for secondary lookup entries (e.g. Department, Designation, Vehicle Type, Fuel Type, Expense Group, Bank)
CREATE TABLE lookup_values (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL, -- e.g. 'DEPARTMENT', 'VEHICLE_TYPE', 'FUEL_TYPE', 'EXPENSE_CATEGORY', 'PAYMENT_METHOD'
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    parent_id BIGINT REFERENCES lookup_values(id), -- Self-reference for hierarchical structure
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    CONSTRAINT uq_lookup_type_code UNIQUE (company_id, type, code)
);

-- 9. Vehicles Table
CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE, -- Vehicle Plate Number / Reg No
    name VARCHAR(150) NOT NULL,
    description TEXT,
    chassis_number VARCHAR(100),
    engine_number VARCHAR(100),
    model VARCHAR(100),
    brand VARCHAR(100),
    type_id BIGINT REFERENCES lookup_values(id),
    category_id BIGINT REFERENCES lookup_values(id),
    capacity_id BIGINT REFERENCES lookup_values(id),
    owner_name VARCHAR(150),
    owner_type VARCHAR(50), -- SELF, HIRED, CLIENT
    purchase_date DATE,
    insurance_expiry_date DATE,
    fitness_expiry_date DATE,
    permit_expiry_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 10. Drivers Table
CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE, -- Driver Employee Code / License No
    name VARCHAR(150) NOT NULL,
    description TEXT,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    license_expiry_date DATE,
    phone_number VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 11. Customers Table
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    gst_number VARCHAR(20),
    credit_limit NUMERIC(15, 2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 12. Suppliers Table
CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    gst_number VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 13. Materials Table
CREATE TABLE materials (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES lookup_values(id),
    unit_id BIGINT REFERENCES lookup_values(id),
    default_rate NUMERIC(12, 2) DEFAULT 0.00,
    density NUMERIC(8, 3) DEFAULT 1.000,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 14. Quarries Table
CREATE TABLE quarries (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    location_address TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);
