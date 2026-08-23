-- Flyway Migration Script: V23__fix_chart_of_accounts_columns.sql
-- Adds missing BaseEntity columns (code, name, status, company_id, branch_id) to chart_of_accounts table

ALTER TABLE chart_of_accounts ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE chart_of_accounts ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE chart_of_accounts ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE chart_of_accounts ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE chart_of_accounts ADD COLUMN IF NOT EXISTS branch_id BIGINT;
