-- ============================================================================
-- V31__add_company_saas_fields.sql
-- Adds owner_name, business_type, and storage capacity limit columns to the 
-- companies table to support advanced SaaS Client Management features.
-- ============================================================================

ALTER TABLE companies ADD COLUMN owner_name VARCHAR(150);
ALTER TABLE companies ADD COLUMN business_type VARCHAR(100);
ALTER TABLE companies ADD COLUMN storage VARCHAR(50) DEFAULT '10 GB';
