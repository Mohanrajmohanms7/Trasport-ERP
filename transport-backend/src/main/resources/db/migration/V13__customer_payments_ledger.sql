-- Flyway Migration Script: V13__customer_payments_ledger.sql
-- Creates database schemas for customer receipts and running ledger balance tables

-- 1. Customer Receipts Table
CREATE TABLE customer_receipts (
    id BIGSERIAL PRIMARY KEY,
    receipt_number VARCHAR(100) NOT NULL UNIQUE,
    receipt_date DATE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    booking_id BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    amount_received NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    advance_amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'CASH' NOT NULL, -- CASH, UPI, GPAY, PHONEPE, NEFT, RTGS, IMPS, BANK_TRANSFER, CHEQUE
    reference_number VARCHAR(100),
    remarks TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_receipt_customer ON customer_receipts(customer_id);
CREATE INDEX idx_receipt_booking ON customer_receipts(booking_id);

-- 2. Customer Ledgers Table
CREATE TABLE customer_ledgers (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    receipt_id BIGINT REFERENCES customer_receipts(id) ON DELETE SET NULL,
    debit_amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    credit_amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    running_balance NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    remarks TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ledger_customer ON customer_ledgers(customer_id);
