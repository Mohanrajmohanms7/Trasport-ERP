-- V81: subscription plans (Starter / Growth / Professional / Enterprise), plan limits, plan history, recorded overrides.

-- Plans: reuse the existing rows (keeps every client's current plan id), add Professional.
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS max_branches INTEGER NOT NULL DEFAULT 1;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS tier INTEGER NOT NULL DEFAULT 0;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS tagline VARCHAR(255);

UPDATE saas_plans SET name = 'Starter', tier = 1, max_users = 3, max_vehicles = 10, max_branches = 1,
       description = 'For small operators: masters, bookings, trips, fuel, expenses, GST invoicing and receipts.',
       tagline = 'Book, run trips, bill and collect' WHERE code = 'BASIC';
UPDATE saas_plans SET name = 'Growth', tier = 2, max_users = 8, max_vehicles = 30, max_branches = 1,
       description = 'Starter + driver payroll, maintenance and work orders, supplier list, settlement dashboard, bulk upload.',
       tagline = 'Pay drivers and keep trucks on the road' WHERE code = 'STANDARD';
UPDATE saas_plans SET name = 'Enterprise', tier = 4, max_users = 0, max_vehicles = 0, max_branches = 0,
       description = 'Everything, multi-branch, GPS and AI insights, no limits, priority support.',
       tagline = 'Run many yards at scale' WHERE code = 'PREMIUM';
UPDATE saas_plans SET tier = 0, max_users = 5, max_vehicles = 10, max_branches = 1,
       description = 'Free trial with Growth features.', tagline = 'Try TransaFlow' WHERE code = 'TRIAL';
INSERT INTO saas_plans (code, name, description, price, billing_period, max_users, max_vehicles, max_invoices, max_branches, tier, tagline)
SELECT 'PROFESSIONAL', 'Professional', 'Growth + spare-part stores, supplier bills & payments, full accounts and financial statements.',
       0.00, 'MONTHLY', 20, 100, 0, 2, 3, 'Run your stores and books in TransaFlow'
WHERE NOT EXISTS (SELECT 1 FROM saas_plans WHERE code = 'PROFESSIONAL');

-- What each plan leaves out (anything not listed is included).
DELETE FROM plan_features WHERE plan_id IN (SELECT id FROM saas_plans WHERE code IN ('TRIAL', 'BASIC', 'STANDARD', 'PROFESSIONAL', 'PREMIUM'));
INSERT INTO plan_features (plan_id, feature_code, enabled, updated_by)
SELECT p.id, f.code, FALSE, 'V81' FROM saas_plans p
JOIN (VALUES
  ('BASIC','suppliers'),('BASIC','fuel.requests'),('BASIC','receipts.dashboard'),('BASIC','payroll'),('BASIC','drivers.salary'),
  ('BASIC','maintenance-requests'),('BASIC','work-orders'),('BASIC','vehicles.maintenance'),('BASIC','vehicles.service-history'),
  ('BASIC','spare-parts'),('BASIC','warehouses'),('BASIC','stock'),('BASIC','inventory-transactions'),('BASIC','payables'),
  ('BASIC','accounts'),('BASIC','financial-statements'),('BASIC','bulk-upload'),('BASIC','ai'),('BASIC','trips.gps'),
  ('BASIC','materials.conversions'),('BASIC','materials.pricing'),('BASIC','materials.locations'),
  ('BASIC','reports.monthly'),('BASIC','reports.vehicles'),('BASIC','reports.drivers'),('BASIC','reports.fuel'),
  ('BASIC','reports.maintenance'),('BASIC','reports.stock'),('BASIC','reports.accounting'),
  ('STANDARD','warehouses'),('STANDARD','stock'),('STANDARD','inventory-transactions'),('STANDARD','payables'),('STANDARD','accounts'),
  ('STANDARD','financial-statements'),('STANDARD','ai'),('STANDARD','trips.gps'),('STANDARD','reports.stock'),('STANDARD','reports.accounting'),
  ('TRIAL','warehouses'),('TRIAL','stock'),('TRIAL','inventory-transactions'),('TRIAL','payables'),('TRIAL','accounts'),
  ('TRIAL','financial-statements'),('TRIAL','ai'),('TRIAL','trips.gps'),('TRIAL','reports.stock'),('TRIAL','reports.accounting'),
  ('PROFESSIONAL','ai'),('PROFESSIONAL','trips.gps')
) AS f(plan_code, code) ON f.plan_code = p.code;

-- Companies: limits follow the plan once Platform Admin applies a plan. Existing clients keep full access and no
-- limits (plan_enforced = false) until a plan is applied to them, so nothing changes for them today.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS max_branches INTEGER;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS plan_enforced BOOLEAN NOT NULL DEFAULT FALSE;

-- Recorded client overrides (add-on, free trial of a module, goodwill…)
ALTER TABLE company_features ADD COLUMN IF NOT EXISTS reason VARCHAR(255);
ALTER TABLE company_features ADD COLUMN IF NOT EXISTS valid_until DATE;

CREATE TABLE IF NOT EXISTS plan_change_history (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    from_plan_id BIGINT REFERENCES saas_plans(id),
    to_plan_id BIGINT NOT NULL REFERENCES saas_plans(id),
    change_type VARCHAR(20) NOT NULL,          -- APPLIED / UPGRADE / DOWNGRADE / SAME
    note VARCHAR(500),
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_plan_change_company ON plan_change_history (company_id, changed_at DESC);
