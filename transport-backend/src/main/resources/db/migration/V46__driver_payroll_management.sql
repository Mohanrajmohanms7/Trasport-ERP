-- Step 10B: Driver Payroll Management Table
CREATE TABLE IF NOT EXISTS driver_payrolls (
    id BIGSERIAL PRIMARY KEY,
    payroll_number VARCHAR(50) NOT NULL UNIQUE,
    driver_id BIGINT NOT NULL REFERENCES drivers(id),
    pay_year INT NOT NULL,
    pay_month INT NOT NULL,
    basic_salary NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    allowance_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    deduction_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    advance_adjustment NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    net_salary_payable NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    accrual_jv_number VARCHAR(100),
    payment_jv_number VARCHAR(100),
    cancellation_jv_number VARCHAR(100),
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    code VARCHAR(50),
    name VARCHAR(150),
    description TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT DEFAULT 0,
    created_by VARCHAR(100),
    created_date TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_date TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_driver_payroll_period UNIQUE (driver_id, pay_year, pay_month)
);

ALTER TABLE driver_payrolls ALTER COLUMN branch_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_driver_payroll_company_status ON driver_payrolls(company_id, status);
CREATE INDEX IF NOT EXISTS idx_driver_payroll_driver ON driver_payrolls(driver_id);
