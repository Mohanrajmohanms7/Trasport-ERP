-- Flyway Migration Script: V41__customer_receipt_reporting_indexes.sql
-- Optimizes query performance for customer receipt lists, allocation history, and audit timelines
-- Creates append-only customer_receipt_audit table for state change histories

-- 1. Create Indexes on customer_receipts
CREATE INDEX IF NOT EXISTS idx_receipts_receipt_date ON customer_receipts(receipt_date);
CREATE INDEX IF NOT EXISTS idx_receipts_customer_id ON customer_receipts(customer_id);
CREATE INDEX IF NOT EXISTS idx_receipts_payment_method ON customer_receipts(payment_method);
CREATE INDEX IF NOT EXISTS idx_receipts_company_id ON customer_receipts(company_id);
CREATE INDEX IF NOT EXISTS idx_receipts_branch_id ON customer_receipts(branch_id);
CREATE INDEX IF NOT EXISTS idx_receipts_comp_branch ON customer_receipts(company_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_receipts_cust_comp_branch ON customer_receipts(customer_id, company_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_receipts_date_comp_branch ON customer_receipts(receipt_date, company_id, branch_id);

-- 2. Create Indexes on sales_invoices
CREATE INDEX IF NOT EXISTS idx_sales_invoices_cust_paystatus ON sales_invoices(customer_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_sales_invoices_comp_branch_paystatus ON sales_invoices(company_id, branch_id, payment_status);

-- 3. Create customer_receipt_audit table
CREATE TABLE IF NOT EXISTS customer_receipt_audit (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT NOT NULL REFERENCES customer_receipts(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by VARCHAR(100) NOT NULL,
    remarks VARCHAR(255),
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL
);

-- 4. Create Indexes on customer_receipt_audit
CREATE INDEX IF NOT EXISTS idx_cr_audit_receipt ON customer_receipt_audit(receipt_id);
CREATE INDEX IF NOT EXISTS idx_cr_audit_event_time ON customer_receipt_audit(event_time);
CREATE INDEX IF NOT EXISTS idx_cr_audit_company ON customer_receipt_audit(company_id);
CREATE INDEX IF NOT EXISTS idx_cr_audit_branch ON customer_receipt_audit(branch_id);
CREATE INDEX IF NOT EXISTS idx_cr_audit_receipt_time ON customer_receipt_audit(receipt_id, event_time);
CREATE INDEX IF NOT EXISTS idx_cr_audit_comp_branch_time ON customer_receipt_audit(company_id, branch_id, event_time);
