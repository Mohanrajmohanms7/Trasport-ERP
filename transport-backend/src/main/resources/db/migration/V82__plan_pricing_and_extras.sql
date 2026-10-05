-- V82: plan pricing (monthly + yearly, extra truck / user / branch prices, setup fee) and per-client extras.
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS price_yearly NUMERIC(12, 2);
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS extra_vehicle_price NUMERIC(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS extra_user_price NUMERIC(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS extra_branch_price NUMERIC(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE saas_plans ADD COLUMN IF NOT EXISTS setup_fee NUMERIC(10, 2) NOT NULL DEFAULT 0;

-- Recommended starting prices (INR). "price" = monthly price; yearly = 10 months (2 months free). Edit any time in
-- Platform Admin → Subscriptions & Plans.
UPDATE saas_plans SET price = 0,     price_yearly = 0,      extra_vehicle_price = 0,   extra_user_price = 0,   extra_branch_price = 0,   setup_fee = 0,    billing_period = 'MONTHLY' WHERE code = 'TRIAL';
UPDATE saas_plans SET price = 1499,  price_yearly = 14990,  extra_vehicle_price = 100, extra_user_price = 199, extra_branch_price = 999, setup_fee = 2999, billing_period = 'MONTHLY' WHERE code = 'BASIC';
UPDATE saas_plans SET price = 3499,  price_yearly = 34990,  extra_vehicle_price = 80,  extra_user_price = 149, extra_branch_price = 999, setup_fee = 4999, billing_period = 'MONTHLY' WHERE code = 'STANDARD';
UPDATE saas_plans SET price = 7999,  price_yearly = 79990,  extra_vehicle_price = 60,  extra_user_price = 99,  extra_branch_price = 799, setup_fee = 7999, billing_period = 'MONTHLY' WHERE code = 'PROFESSIONAL';
UPDATE saas_plans SET price = 14999, price_yearly = 149990, extra_vehicle_price = 0,   extra_user_price = 0,   extra_branch_price = 0,   setup_fee = 0,    billing_period = 'MONTHLY' WHERE code = 'PREMIUM';

-- Extras bought by a client on top of its plan, and how it pays.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS extra_vehicles INTEGER NOT NULL DEFAULT 0;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS extra_users INTEGER NOT NULL DEFAULT 0;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS extra_branches INTEGER NOT NULL DEFAULT 0;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(10) NOT NULL DEFAULT 'MONTHLY';
