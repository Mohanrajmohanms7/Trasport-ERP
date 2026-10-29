-- V89: flexible driver pay periods (Part 2): a payroll covers period_from..period_to inside one calendar month
-- (e.g. 1-10, 11-20, 21-31 or the whole month). Existing payrolls cover their whole month (unchanged).
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS period_from DATE;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS period_to DATE;
UPDATE driver_payrolls SET period_from = make_date(pay_year, pay_month, 1),
                           period_to = (make_date(pay_year, pay_month, 1) + INTERVAL '1 month' - INTERVAL '1 day')::date
 WHERE period_from IS NULL OR period_to IS NULL;
ALTER TABLE driver_payrolls ALTER COLUMN period_from SET NOT NULL;
ALTER TABLE driver_payrolls ALTER COLUMN period_to SET NOT NULL;
ALTER TABLE driver_payrolls DROP CONSTRAINT IF EXISTS chk_driver_payroll_period;
ALTER TABLE driver_payrolls ADD CONSTRAINT chk_driver_payroll_period CHECK (period_from <= period_to
    AND date_trunc('month', period_from) = date_trunc('month', period_to)
    AND EXTRACT(YEAR FROM period_from) = pay_year AND EXTRACT(MONTH FROM period_from) = pay_month);
-- One active payroll per driver and month is replaced by "no overlapping active periods" (checked by the service under
-- a per-driver lock); the same start day can never be active twice.
DROP INDEX IF EXISTS uq_driver_payroll_active_period;
CREATE UNIQUE INDEX IF NOT EXISTS uq_driver_payroll_active_start
    ON driver_payrolls (driver_id, period_from) WHERE is_deleted = FALSE AND status <> 'CANCELLED';
CREATE INDEX IF NOT EXISTS idx_driver_payroll_driver_period ON driver_payrolls (driver_id, period_from, period_to);
