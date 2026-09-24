-- V61: GST-correct invoice totals, sequential document numbers, trip weighbridge/quarry data.

-- 1. Sequential document numbers per company, document type and Indian financial year.
CREATE TABLE IF NOT EXISTS document_sequences (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    doc_type VARCHAR(30) NOT NULL,
    fy_label VARCHAR(10) NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 1,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_document_sequences_company_type_fy UNIQUE (company_id, doc_type, fy_label)
);

-- 2. Invoice header: taxable value and tax kept separately; place of supply decides CGST+SGST vs IGST.
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS taxable_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS supply_type VARCHAR(20) NOT NULL DEFAULT 'INTRA_STATE';
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS place_of_supply VARCHAR(2);

-- 3. Invoice lines: discount share and post-discount taxable value.
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS taxable_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00;

-- Backfill existing rows from what was stored (history is not recalculated).
UPDATE sales_invoice_details
   SET taxable_amount = net_amount - (COALESCE(cgst, 0) + COALESCE(sgst, 0) + COALESCE(igst, 0));

UPDATE sales_invoices i
   SET tax_amount = COALESCE((
         SELECT SUM(COALESCE(d.cgst, 0) + COALESCE(d.sgst, 0) + COALESCE(d.igst, 0))
           FROM sales_invoice_details d
          WHERE d.invoice_id = i.id AND d.is_deleted = FALSE), 0);

UPDATE sales_invoices SET taxable_amount = net_amount - tax_amount;

-- 4. Trips: loading source.
ALTER TABLE trips ADD COLUMN IF NOT EXISTS quarry_id BIGINT REFERENCES quarries(id);
ALTER TABLE trips ADD COLUMN IF NOT EXISTS loading_location_id BIGINT REFERENCES loading_locations(id);
CREATE INDEX IF NOT EXISTS idx_trips_company_quarry ON trips (company_id, quarry_id);
CREATE INDEX IF NOT EXISTS idx_trips_company_booking_status ON trips (company_id, booking_id, status);

-- 5. Trip lines: weighbridge loaded vs delivered quantity (shortage = loaded - delivered).
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS loaded_quantity NUMERIC(12, 2);
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS delivered_quantity NUMERIC(12, 2);
