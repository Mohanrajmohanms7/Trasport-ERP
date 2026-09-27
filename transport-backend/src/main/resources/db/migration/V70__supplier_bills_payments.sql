-- V70: Accounts payable per supplier — bills (manual or created by credit stock receipts / workshop jobs),
-- payments allocated to bills, and supplier credit terms.

ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS credit_days INT;

CREATE TABLE IF NOT EXISTS supplier_bills (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',          -- DRAFT, APPROVED, CANCELLED
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    bill_number VARCHAR(50) NOT NULL,                     -- our number (SB-2627/00001)
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    supplier_bill_no VARCHAR(60),                         -- supplier's invoice number
    bill_date DATE NOT NULL,
    due_date DATE NOT NULL,
    category VARCHAR(20) NOT NULL,                        -- PARTS_STOCK, WORKSHOP, REPAIR, TYRES, FUEL, OFFICE, OTHER
    taxable_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    gst_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',  -- UNPAID, PARTIALLY_PAID, PAID
    source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL',    -- MANUAL, STOCK_RECEIPT, WORK_ORDER
    source_id BIGINT,
    source_reference VARCHAR(100),
    vehicle_id BIGINT REFERENCES vehicles(id),
    jv_reference VARCHAR(100),
    remarks TEXT,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_supplier_bills_amounts CHECK (total_amount >= 0 AND paid_amount >= 0 AND paid_amount <= total_amount)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_supplier_bills_number ON supplier_bills (company_id, bill_number) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_supplier_bills_source ON supplier_bills (company_id, source_type, source_id)
    WHERE is_deleted = FALSE AND source_id IS NOT NULL AND status <> 'CANCELLED';
CREATE INDEX IF NOT EXISTS idx_supplier_bills_supplier ON supplier_bills (company_id, supplier_id, status, payment_status);

CREATE TABLE IF NOT EXISTS supplier_payments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'POSTED',         -- POSTED, CANCELLED
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    payment_number VARCHAR(50) NOT NULL,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    payment_date DATE NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,                  -- CASH, BANK_TRANSFER, CHEQUE, UPI
    reference_number VARCHAR(100),
    jv_reference VARCHAR(100),
    remarks TEXT,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_supplier_payments_amount CHECK (amount > 0)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_supplier_payments_number ON supplier_payments (company_id, payment_number) WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS supplier_payment_allocations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',         -- ACTIVE, REVERSED
    company_id BIGINT,
    branch_id BIGINT,
    payment_id BIGINT NOT NULL REFERENCES supplier_payments(id),
    bill_id BIGINT NOT NULL REFERENCES supplier_bills(id),
    amount NUMERIC(14, 2) NOT NULL,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_supplier_alloc_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_supplier_alloc_payment ON supplier_payment_allocations (payment_id);
CREATE INDEX IF NOT EXISTS idx_supplier_alloc_bill ON supplier_payment_allocations (bill_id);
