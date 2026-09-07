-- Flyway Migration Script: V49__document_upload_and_export_hardening.sql
-- Creates schema for Trip POD & Attachment documents and adds attachment columns to fuel, expense, and service tables

-- 1. Trip Documents Table (POD, Delivery Challan, Weighbridge Slips, Photos)
CREATE TABLE IF NOT EXISTS trip_documents (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    doc_type VARCHAR(50) NOT NULL, -- POD, DELIVERY_CHALLAN, WEIGHBRIDGE_SLIP, LOADING_PHOTO, DELIVERY_PHOTO, OTHER
    doc_number VARCHAR(100),
    file_path TEXT NOT NULL,
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    file_size BIGINT,
    remarks TEXT,
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_trip_doc_trip ON trip_documents(trip_id);
CREATE INDEX IF NOT EXISTS idx_trip_doc_company ON trip_documents(company_id);

-- 2. Add attachment_path column to fuel_entries if not exists
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS attachment_path TEXT;

-- 3. Add attachment_path column to expenses if not exists
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS attachment_path TEXT;

-- 4. Add attachment_path column to vehicle_services if not exists
ALTER TABLE vehicle_services ADD COLUMN IF NOT EXISTS attachment_path TEXT;
