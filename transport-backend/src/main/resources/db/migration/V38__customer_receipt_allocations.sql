-- Flyway Migration Script: V38__customer_receipt_allocations.sql
-- Alters sales_invoices to add payment status columns
-- Creates customer_receipt_allocations mapping table

-- 1. Add columns to sales_invoices
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(15, 2) DEFAULT 0.00 NOT NULL;
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) DEFAULT 'UNPAID' NOT NULL;

-- 2. Create customer_receipt_allocations mapping table
CREATE TABLE customer_receipt_allocations (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT NOT NULL REFERENCES customer_receipts(id) ON DELETE CASCADE,
    invoice_id BIGINT NOT NULL REFERENCES sales_invoices(id) ON DELETE CASCADE,
    allocated_amount NUMERIC(12, 2) NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alloc_receipt ON customer_receipt_allocations(receipt_id);
CREATE INDEX idx_alloc_invoice ON customer_receipt_allocations(invoice_id);
CREATE INDEX idx_alloc_company_branch ON customer_receipt_allocations(company_id, branch_id);
