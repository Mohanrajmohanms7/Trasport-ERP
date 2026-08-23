-- V35: App settings must be per-company (SaaS onboarding)
--
-- POST /platform-admin/onboard failed with 409 because copying template
-- settings reuses the same key_name values, but app_settings_key_name_key
-- enforced a global unique on key_name.

ALTER TABLE app_settings DROP CONSTRAINT IF EXISTS app_settings_key_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_settings_company_key
    ON app_settings (company_id, key_name)
    WHERE is_deleted = false;
