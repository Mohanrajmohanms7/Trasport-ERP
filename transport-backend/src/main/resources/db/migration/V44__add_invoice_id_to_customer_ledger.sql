-- Flyway Migration Script: V44__add_invoice_id_to_customer_ledger.sql
-- Adds invoice_id reference column to customer_ledgers table for accounting traceability

ALTER TABLE customer_ledgers
ADD COLUMN invoice_id BIGINT REFERENCES sales_invoices(id) ON DELETE SET NULL;

CREATE INDEX idx_ledger_invoice ON customer_ledgers(invoice_id);
