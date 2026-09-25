-- V64: files kept in the database (survive redeploys on hosts without a persistent disk),
-- generic document attachments, and photos for vehicles, drivers and spare parts.

CREATE TABLE IF NOT EXISTS stored_files (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_id BIGINT,
    branch_id BIGINT,
    stored_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    content BYTEA NOT NULL,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_stored_files_name ON stored_files (stored_name);

CREATE TABLE IF NOT EXISTS attachments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    category VARCHAR(40),
    remarks TEXT,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_attachments_entity ON attachments (company_id, entity_type, entity_id, is_deleted);

ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS photo_file VARCHAR(255);
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS photo_file VARCHAR(255);
ALTER TABLE spare_parts ADD COLUMN IF NOT EXISTS photo_file VARCHAR(255);
