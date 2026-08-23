-- Flyway Migration Script: V27__fix_fuel_tables_base_entity_columns.sql
-- Adds missing BaseEntity columns (company_id, branch_id) to fuel_entries and fuel_requests tables

-- 1. fuel_entries table
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 2. fuel_requests table
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS branch_id BIGINT;
