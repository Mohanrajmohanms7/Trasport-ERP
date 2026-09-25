-- V62: Driver Daily Slab Payroll, payroll day breakdown, deductions, driver advances.

-- 1. Company-level daily pay slabs: trips_from..trips_to completed trips in one day -> daily_amount.
CREATE TABLE IF NOT EXISTS driver_pay_slabs (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    trips_from INT NOT NULL,
    trips_to INT,
    daily_amount NUMERIC(12, 2) NOT NULL,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_driver_pay_slabs_range CHECK (trips_from >= 1 AND (trips_to IS NULL OR trips_to >= trips_from)),
    CONSTRAINT ck_driver_pay_slabs_amount CHECK (daily_amount >= 0)
);
CREATE INDEX IF NOT EXISTS idx_driver_pay_slabs_company ON driver_pay_slabs (company_id, is_deleted);

-- 2. Payroll header: trip-based figures, gross, approval / posting / payment audit.
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS total_trips INT NOT NULL DEFAULT 0;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS trip_days INT NOT NULL DEFAULT 0;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS trip_earnings NUMERIC(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100);
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS posting_date DATE;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS posted_by VARCHAR(100);
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS posted_at TIMESTAMP;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS recovery_jv_number VARCHAR(100);
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS deduction_jv_number VARCHAR(100);
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS paid_date DATE;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS paid_by VARCHAR(100);
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100);

UPDATE driver_payrolls SET gross_amount = basic_salary + allowance_amount WHERE gross_amount = 0;
-- Before V62, APPROVED already meant "accrual JV posted"; that is POSTED now.
UPDATE driver_payrolls SET status = 'POSTED' WHERE status = 'APPROVED' AND accrual_jv_number IS NOT NULL;

-- One ACTIVE payroll per driver and month. Deleted / cancelled ones no longer block a redo.
ALTER TABLE driver_payrolls DROP CONSTRAINT IF EXISTS uk_driver_payroll_period;
CREATE UNIQUE INDEX IF NOT EXISTS uq_driver_payroll_active_period
    ON driver_payrolls (driver_id, pay_year, pay_month)
    WHERE is_deleted = FALSE AND status <> 'CANCELLED';
CREATE INDEX IF NOT EXISTS idx_driver_payroll_company_period ON driver_payrolls (company_id, pay_year, pay_month);

-- 3. Day-by-day breakdown kept with the payroll (audit / salary slip).
CREATE TABLE IF NOT EXISTS driver_payroll_days (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_id BIGINT,
    branch_id BIGINT,
    payroll_id BIGINT NOT NULL REFERENCES driver_payrolls(id),
    work_date DATE NOT NULL,
    trip_count INT NOT NULL,
    slab_trips_from INT,
    slab_trips_to INT,
    daily_amount NUMERIC(12, 2) NOT NULL,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_driver_payroll_days_payroll ON driver_payroll_days (payroll_id);

-- 4. Itemised deductions (fine / damage / other). Advance recovery is tracked separately below.
CREATE TABLE IF NOT EXISTS driver_payroll_deductions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_id BIGINT,
    branch_id BIGINT,
    payroll_id BIGINT NOT NULL REFERENCES driver_payrolls(id),
    deduction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    deduction_date DATE NOT NULL,
    remarks TEXT,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_driver_payroll_deductions_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_driver_payroll_deductions_payroll ON driver_payroll_deductions (payroll_id);

-- 5. Driver advances: cash given to a driver, recovered through payroll.
CREATE TABLE IF NOT EXISTS driver_advances (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED', -- ISSUED, CANCELLED
    company_id BIGINT NOT NULL,
    branch_id BIGINT,
    advance_number VARCHAR(50) NOT NULL,
    driver_id BIGINT NOT NULL REFERENCES drivers(id),
    advance_date DATE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    recovered_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(100),
    jv_number VARCHAR(100),
    cancellation_jv_number VARCHAR(100),
    remarks TEXT,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_driver_advances_amount CHECK (amount > 0),
    CONSTRAINT ck_driver_advances_recovered CHECK (recovered_amount >= 0 AND recovered_amount <= amount)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_driver_advances_company_number
    ON driver_advances (company_id, advance_number) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_driver_advances_driver_status ON driver_advances (company_id, driver_id, status);

-- 6. Which advance each posted payroll recovered, and how much (reversed on payroll cancellation).
CREATE TABLE IF NOT EXISTS driver_advance_recoveries (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, REVERSED
    company_id BIGINT,
    branch_id BIGINT,
    payroll_id BIGINT NOT NULL REFERENCES driver_payrolls(id),
    advance_id BIGINT NOT NULL REFERENCES driver_advances(id),
    amount NUMERIC(12, 2) NOT NULL,
    created_by VARCHAR(50),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_driver_advance_recoveries_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS idx_driver_advance_recoveries_payroll ON driver_advance_recoveries (payroll_id);
CREATE INDEX IF NOT EXISTS idx_driver_advance_recoveries_advance ON driver_advance_recoveries (advance_id);

-- 7. Trip lookups for payroll: driver + business date + status.
CREATE INDEX IF NOT EXISTS idx_trips_company_driver_date_status ON trips (company_id, driver_id, trip_date, status);

-- 8. Payroll numbers become per-company financial-year sequences (PAY-2627/00001), so uniqueness is per company.
ALTER TABLE driver_payrolls DROP CONSTRAINT IF EXISTS driver_payrolls_payroll_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_driver_payrolls_company_number
    ON driver_payrolls (company_id, payroll_number) WHERE is_deleted = FALSE;
