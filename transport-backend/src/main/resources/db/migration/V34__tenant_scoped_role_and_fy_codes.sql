-- V34: Allow per-tenant role codes and financial year codes (SaaS onboarding)
--
-- Root cause of POST /platform-admin/onboard 500:
--   app_roles.code was UNIQUE globally, so creating COMPANY_ADMIN / OPERATOR / etc.
--   for a second company violated app_roles_code_key.
-- Same issue for financial_years.code (FY2026-27 already exists for company 1).

ALTER TABLE app_roles DROP CONSTRAINT IF EXISTS app_roles_code_key;

-- System roles (company_id NULL) keep unique codes; tenant roles unique per company.
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_roles_system_code
    ON app_roles (code)
    WHERE company_id IS NULL AND is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_roles_tenant_code
    ON app_roles (company_id, code)
    WHERE company_id IS NOT NULL AND is_deleted = false;

ALTER TABLE financial_years DROP CONSTRAINT IF EXISTS financial_years_code_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_financial_years_company_code
    ON financial_years (company_id, code)
    WHERE is_deleted = false;
