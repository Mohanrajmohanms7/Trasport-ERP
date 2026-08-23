-- Flyway Migration Script: V15__accounts_general_ledger.sql
-- Creates database schemas for Chart of Accounts and Journal Vouchers tables

-- 1. Chart of Accounts Table
CREATE TABLE chart_of_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_code VARCHAR(100) NOT NULL UNIQUE,
    account_name VARCHAR(200) NOT NULL,
    account_type VARCHAR(100) NOT NULL, -- ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
    opening_balance NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    running_balance NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Journal Vouchers Table (Double-Entry Vouchers)
CREATE TABLE journal_vouchers (
    id BIGSERIAL PRIMARY KEY,
    voucher_number VARCHAR(100) NOT NULL UNIQUE,
    voucher_date DATE NOT NULL,
    reference_number VARCHAR(100),
    description TEXT,
    debit_account_id BIGINT NOT NULL REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
    credit_account_id BIGINT NOT NULL REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_journal_debit ON journal_vouchers(debit_account_id);
CREATE INDEX idx_journal_credit ON journal_vouchers(credit_account_id);
