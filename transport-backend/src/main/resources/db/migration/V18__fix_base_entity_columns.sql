-- Flyway Migration Script: V18__fix_base_entity_columns.sql
-- Adds missing BaseEntity columns to report_templates, scheduled_reports, gps_trackings, and ai_predictions

-- 1. report_templates table
ALTER TABLE report_templates ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE report_templates ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE report_templates ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE report_templates ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE report_templates ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE report_templates ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 2. scheduled_reports table
ALTER TABLE scheduled_reports ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE scheduled_reports ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE scheduled_reports ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE scheduled_reports ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE scheduled_reports ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE scheduled_reports ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 3. gps_trackings table
ALTER TABLE gps_trackings ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE gps_trackings ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE gps_trackings ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE gps_trackings ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE gps_trackings ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE gps_trackings ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 4. ai_predictions table
ALTER TABLE ai_predictions ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE ai_predictions ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE ai_predictions ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE ai_predictions ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE ai_predictions ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE ai_predictions ADD COLUMN IF NOT EXISTS branch_id BIGINT;
