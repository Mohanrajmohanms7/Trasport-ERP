-- V85: client-specific invoice quantity basis (set by Platform Admin only).
--   DELIVERED (default, unchanged behaviour): bill the delivered quantity when recorded, else the planned quantity.
--   ORDERED: bill each trip's planned (ordered) quantity, or the delivered quantity when more was delivered.
-- The delivered quantity is always kept on the trip and copied to the invoice line, and shown on the invoice.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS billing_quantity_basis VARCHAR(20) NOT NULL DEFAULT 'DELIVERED';
ALTER TABLE companies DROP CONSTRAINT IF EXISTS chk_companies_billing_quantity_basis;
ALTER TABLE companies ADD CONSTRAINT chk_companies_billing_quantity_basis CHECK (billing_quantity_basis IN ('DELIVERED', 'ORDERED'));

ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS delivered_quantity NUMERIC(12, 3);
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS quantity_basis VARCHAR(20);

-- The one quantity rule, shared by bookings, invoices and reports.
CREATE OR REPLACE FUNCTION billed_qty(planned NUMERIC, delivered NUMERIC, p_company_id BIGINT) RETURNS NUMERIC
LANGUAGE sql STABLE AS $$
    SELECT CASE
             WHEN (SELECT billing_quantity_basis FROM companies WHERE id = p_company_id) = 'ORDERED'
               THEN GREATEST(COALESCE(planned, 0), COALESCE(delivered, 0))
             ELSE COALESCE(NULLIF(delivered, 0), planned, 0)
           END
$$;
