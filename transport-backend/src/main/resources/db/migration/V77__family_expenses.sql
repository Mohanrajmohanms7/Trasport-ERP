-- V77: Family Expenses (optional add-on, off by default; switched on per client in Platform Admin → Feature Access).
-- A personal register kept completely apart from the business: no vehicle / driver / trip links, no journal entries,
-- no effect on cash / bank balances, P&L, dashboard or any business report. See docs/FAMILY_EXPENSES.md.
-- Categories are lookup_values of type FAMILY_EXPENSE_CATEGORY (seeded for a company when it first opens the module).

CREATE TABLE family_expenses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    branch_id BIGINT,
    expense_number VARCHAR(50) NOT NULL,
    expense_date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    payment_mode VARCHAR(50) NOT NULL,
    member_name VARCHAR(100),
    reference_no VARCHAR(100),
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uq_family_expenses_number ON family_expenses (company_id, expense_number);
CREATE INDEX idx_family_expenses_company_date ON family_expenses (company_id, expense_date) WHERE is_deleted = false;
CREATE INDEX idx_family_expenses_company_category ON family_expenses (company_id, category) WHERE is_deleted = false;
