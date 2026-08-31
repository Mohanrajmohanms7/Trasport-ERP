-- Flyway Migration Script: V43__customer_receipt_settlement_reporting.sql
-- Optimizations for Customer Receipt Settlement Dashboard and Aging Analysis

CREATE INDEX IF NOT EXISTS idx_customer_receipts_reporting_p37 
    ON customer_receipts(customer_id, status, receipt_date);

CREATE INDEX IF NOT EXISTS idx_customer_receipts_branch_reporting_p37 
    ON customer_receipts(company_id, branch_id, status, receipt_date);

CREATE INDEX IF NOT EXISTS idx_receipt_allocations_reconcile_p37 
    ON customer_receipt_allocations(receipt_id, invoice_id);

CREATE INDEX IF NOT EXISTS idx_receipt_allocations_inv_p37 
    ON customer_receipt_allocations(invoice_id);

CREATE INDEX IF NOT EXISTS idx_sales_invoices_aging_cust_p37 
    ON sales_invoices(customer_id, invoice_date);

CREATE INDEX IF NOT EXISTS idx_sales_invoices_aging_branch_p37 
    ON sales_invoices(company_id, branch_id, invoice_date);
