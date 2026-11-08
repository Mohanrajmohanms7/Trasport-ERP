-- V91: customer-paid driver bata per trip. The customer pays this directly to the driver; it is shown on the invoice PDF
-- for information only and is NEVER part of the invoice amounts, GST, receivables, receipts, accounts or driver payroll.
ALTER TABLE trips ADD COLUMN IF NOT EXISTS customer_paid_bata NUMERIC(12,2);
ALTER TABLE trips DROP CONSTRAINT IF EXISTS chk_trip_customer_paid_bata;
ALTER TABLE trips ADD CONSTRAINT chk_trip_customer_paid_bata CHECK (customer_paid_bata IS NULL OR customer_paid_bata >= 0);
