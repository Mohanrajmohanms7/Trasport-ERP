-- Flyway Migration Script: V50__fix_trip_documents_base_entity_columns.sql
-- Adds missing BaseEntity columns (code, name, description, status) to trip_documents table

ALTER TABLE trip_documents ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE trip_documents ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE trip_documents ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE trip_documents ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
