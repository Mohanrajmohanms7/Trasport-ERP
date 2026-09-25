-- V63: driver bata already paid through expenses during the pay month (information on payroll / salary slip).
-- Not part of gross or net: bata is expensed when the expense is approved.
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS bata_paid NUMERIC(12, 2) NOT NULL DEFAULT 0.00;
CREATE INDEX IF NOT EXISTS idx_expenses_company_driver_category_date ON expenses (company_id, driver_id, category, expense_date);
