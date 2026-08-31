-- Flyway Migration Script: V42__customer_receipt_print_audit.sql
-- Creates the customer_receipt_print_audit table and indexes for tracking print and PDF export events

CREATE TABLE IF NOT EXISTS customer_receipt_print_audit (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT NOT NULL REFERENCES customer_receipts(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL, -- PRINT, PDF_EXPORT, REPRINT
    printed_by VARCHAR(100) NOT NULL,
    printed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    format VARCHAR(20) NOT NULL, -- PRINT, PDF
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    
    -- Framework BaseEntity metadata fields
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    updated_by VARCHAR(100) DEFAULT 'system',
    version INT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL
);

-- Index optimizations
CREATE INDEX IF NOT EXISTS idx_receipt_print_audit_receipt_id ON customer_receipt_print_audit(receipt_id);
CREATE INDEX IF NOT EXISTS idx_receipt_print_audit_company_id ON customer_receipt_print_audit(company_id);
CREATE INDEX IF NOT EXISTS idx_receipt_print_audit_branch_id ON customer_receipt_print_audit(branch_id);
CREATE INDEX IF NOT EXISTS idx_receipt_print_audit_printed_at ON customer_receipt_print_audit(printed_at);
