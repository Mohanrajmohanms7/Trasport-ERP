-- Flyway Migration Script: V22__fix_header_tables_code_name.sql
-- Adds missing BaseEntity columns (code, name) to all transaction header tables in the system

-- 1. bookings table
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS name VARCHAR(150);

-- 2. trips table
ALTER TABLE trips ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS name VARCHAR(150);

-- 3. fuel_entries table
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS name VARCHAR(150);

-- 4. fuel_requests table
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS name VARCHAR(150);

-- 5. expenses table
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS name VARCHAR(150);

-- 6. customer_receipts table
ALTER TABLE customer_receipts ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE customer_receipts ADD COLUMN IF NOT EXISTS name VARCHAR(150);

-- 7. sales_invoices table
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS name VARCHAR(150);

-- 8. journal_vouchers table
ALTER TABLE journal_vouchers ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE journal_vouchers ADD COLUMN IF NOT EXISTS name VARCHAR(150);
