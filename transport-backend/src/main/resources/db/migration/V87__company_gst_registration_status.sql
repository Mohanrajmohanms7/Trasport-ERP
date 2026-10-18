-- V87: GST registration status on companies (REGISTERED / UNREGISTERED / PENDING). GSTIN is required only when REGISTERED.
-- Existing clients keep their behaviour: a company with a GSTIN becomes REGISTERED (GSTIN untouched), one without becomes
-- UNREGISTERED. Empty GSTINs are stored as NULL. No GSTIN is created or changed.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS gst_registration_status VARCHAR(20);
UPDATE companies SET gst_number = NULL WHERE gst_number IS NOT NULL AND TRIM(gst_number) = '';
UPDATE companies SET gst_registration_status = CASE WHEN gst_number IS NOT NULL THEN 'REGISTERED' ELSE 'UNREGISTERED' END
 WHERE gst_registration_status IS NULL;
ALTER TABLE companies ALTER COLUMN gst_registration_status SET DEFAULT 'UNREGISTERED';
ALTER TABLE companies ALTER COLUMN gst_registration_status SET NOT NULL;
ALTER TABLE companies DROP CONSTRAINT IF EXISTS chk_companies_gst_registration_status;
ALTER TABLE companies ADD CONSTRAINT chk_companies_gst_registration_status
    CHECK (gst_registration_status IN ('REGISTERED', 'UNREGISTERED', 'PENDING'));
