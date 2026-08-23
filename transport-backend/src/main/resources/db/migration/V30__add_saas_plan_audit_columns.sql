-- ============================================================================
-- V30__add_saas_plan_audit_columns.sql
-- Adds missing audit validation columns (is_deleted, version) to saas_plans.
-- ============================================================================

ALTER TABLE saas_plans ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE saas_plans ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
