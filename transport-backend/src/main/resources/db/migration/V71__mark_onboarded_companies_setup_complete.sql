-- V71: clients created through Platform Admin onboarding are fully provisioned (head office, financial year,
-- roles, admin, masters). Mark them as set up so their users are not sent to the Setup Wizard on first login.
INSERT INTO app_settings (key_name, value_data, description, code, name, status, company_id, branch_id,
                          created_by, created_date, updated_by, updated_date, is_deleted, version)
SELECT 'SETUP_COMPLETED', 'true', 'Marked complete for provisioned company (V71)', 'SETUP_COMPLETED', 'Setup Completed', 'ACTIVE',
       c.id, (SELECT MIN(b.id) FROM branches b WHERE b.company_id = c.id AND b.is_deleted = FALSE),
       'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, FALSE, 0
FROM companies c
WHERE c.is_deleted = FALSE
  AND EXISTS (SELECT 1 FROM branches b WHERE b.company_id = c.id AND b.is_deleted = FALSE)
  AND EXISTS (SELECT 1 FROM financial_years f WHERE f.company_id = c.id AND f.is_deleted = FALSE)
  AND NOT EXISTS (SELECT 1 FROM app_settings s WHERE s.company_id = c.id AND s.key_name = 'SETUP_COMPLETED' AND s.is_deleted = FALSE);

UPDATE app_settings s SET value_data = 'true', updated_by = 'SYSTEM', updated_date = CURRENT_TIMESTAMP
WHERE s.key_name = 'SETUP_COMPLETED' AND s.is_deleted = FALSE AND s.value_data <> 'true'
  AND EXISTS (SELECT 1 FROM financial_years f WHERE f.company_id = s.company_id AND f.is_deleted = FALSE);
