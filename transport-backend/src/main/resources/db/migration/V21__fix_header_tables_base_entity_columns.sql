-- Flyway Migration Script: V21__fix_header_tables_base_entity_columns.sql
-- Adds missing BaseEntity columns (company_id, branch_id) to all transaction header tables in the system

-- 1. bookings table
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 2. trips table
ALTER TABLE trips ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE trips ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 3. expenses table
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 4. customer_receipts table
ALTER TABLE customer_receipts ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE customer_receipts ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 5. sales_invoices table
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 6. journal_vouchers table
ALTER TABLE journal_vouchers ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE journal_vouchers ADD COLUMN IF NOT EXISTS branch_id BIGINT;
