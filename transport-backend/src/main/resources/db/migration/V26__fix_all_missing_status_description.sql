-- Flyway Migration Script: V26__fix_all_missing_status_description.sql
-- Adds missing status and description columns to header tables

-- 1. trips table
ALTER TABLE trips ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE trips ADD COLUMN IF NOT EXISTS description TEXT;

-- 2. fuel_entries table
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS description TEXT;

-- 3. fuel_requests table
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS description TEXT;

-- 4. expenses table
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS description TEXT;

-- 5. customer_receipts table
ALTER TABLE customer_receipts ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE customer_receipts ADD COLUMN IF NOT EXISTS description TEXT;

-- 6. sales_invoices table
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS description TEXT;

-- 7. journal_vouchers table
ALTER TABLE journal_vouchers ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE journal_vouchers ADD COLUMN IF NOT EXISTS description TEXT;
