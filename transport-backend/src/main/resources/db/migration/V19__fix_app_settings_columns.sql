-- Flyway Migration Script: V19__fix_app_settings_columns.sql
-- Adds missing BaseEntity columns to app_settings table

-- 5. app_settings table
ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
