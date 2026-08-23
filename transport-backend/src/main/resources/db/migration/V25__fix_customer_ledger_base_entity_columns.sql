-- Flyway Migration Script: V25__fix_customer_ledger_base_entity_columns.sql
-- Adds missing BaseEntity columns (code, name, status, company_id, branch_id) to customer_ledgers table

ALTER TABLE customer_ledgers ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE customer_ledgers ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE customer_ledgers ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE customer_ledgers ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE customer_ledgers ADD COLUMN IF NOT EXISTS branch_id BIGINT;
