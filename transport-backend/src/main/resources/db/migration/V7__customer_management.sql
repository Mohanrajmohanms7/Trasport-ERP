-- Flyway Migration Script: V7__customer_management.sql
-- Creates database schemas for customer contacts directory, delivery site locations, and legal documents scans

-- 1. Customer Contacts Table
CREATE TABLE customer_contacts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    contact_name VARCHAR(150) NOT NULL,
    designation VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_customer_contact_cust ON customer_contacts(customer_id);

-- 2. Customer Delivery Sites Table
CREATE TABLE customer_delivery_sites (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    site_code VARCHAR(50) NOT NULL,
    site_name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    manager_name VARCHAR(100),
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_customer_site_cust ON customer_delivery_sites(customer_id);

-- 3. Customer Documents Table
CREATE TABLE customer_documents (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    doc_type VARCHAR(50) NOT NULL, -- GST_CERT, PAN_CARD, KYC, AGREEMENT
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
CREATE INDEX idx_customer_doc_cust ON customer_documents(customer_id);
