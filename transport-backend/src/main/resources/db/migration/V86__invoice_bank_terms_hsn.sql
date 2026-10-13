-- V86: invoice PDF "Design A" — optional bank / UPI / terms on the company, HSN/SAC on materials.
-- All optional: the invoice prints with the space blank when a value is empty.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS bank_name VARCHAR(120);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS bank_branch VARCHAR(120);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(40);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS bank_ifsc VARCHAR(20);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS upi_id VARCHAR(80);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS invoice_terms TEXT;
ALTER TABLE materials ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(12);
