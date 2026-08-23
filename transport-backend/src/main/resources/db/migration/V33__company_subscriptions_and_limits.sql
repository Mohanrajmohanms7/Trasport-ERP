-- ============================================================================
-- V33__company_subscriptions_and_limits.sql
-- Adds subscription plan, start/end dates, renewal dates, and user/vehicle 
-- limits directly to the companies table for fast validation.
-- ============================================================================

ALTER TABLE companies ADD COLUMN subscription_plan_id BIGINT;
ALTER TABLE companies ADD COLUMN subscription_start_date DATE;
ALTER TABLE companies ADD COLUMN subscription_end_date DATE;
ALTER TABLE companies ADD COLUMN subscription_renewal_date DATE;
ALTER TABLE companies ADD COLUMN subscription_status VARCHAR(30) DEFAULT 'ACTIVE';
ALTER TABLE companies ADD COLUMN max_users INTEGER DEFAULT 100;
ALTER TABLE companies ADD COLUMN max_vehicles INTEGER DEFAULT 100;

ALTER TABLE companies ADD CONSTRAINT fk_company_subscription_plan FOREIGN KEY (subscription_plan_id) REFERENCES saas_plans(id);

-- Apply plan defaults to any existing companies (dynamic — no hardcoded company ids)
UPDATE companies SET
    subscription_plan_id = COALESCE(
        subscription_plan_id,
        (SELECT id FROM saas_plans WHERE code = 'TRIAL' AND is_deleted = false LIMIT 1),
        (SELECT id FROM saas_plans WHERE code = 'BASIC' AND is_deleted = false LIMIT 1),
        (SELECT id FROM saas_plans WHERE status = 'ACTIVE' AND is_deleted = false ORDER BY id LIMIT 1)
    ),
    subscription_start_date = COALESCE(subscription_start_date, CURRENT_DATE),
    subscription_end_date = COALESCE(subscription_end_date, CURRENT_DATE + INTERVAL '1 year'),
    subscription_renewal_date = COALESCE(subscription_renewal_date, CURRENT_DATE + INTERVAL '1 year'),
    subscription_status = COALESCE(subscription_status, 'ACTIVE'),
    max_users = COALESCE(max_users, 5),
    max_vehicles = COALESCE(max_vehicles, 5)
WHERE subscription_plan_id IS NULL;
