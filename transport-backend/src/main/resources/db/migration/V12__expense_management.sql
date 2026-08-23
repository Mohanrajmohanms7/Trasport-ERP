-- Flyway Migration Script: V12__expense_management.sql
-- Creates database schemas for expense entries and payment status tracking tables

-- 1. Expenses Table
CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    expense_number VARCHAR(100) NOT NULL UNIQUE,
    expense_date DATE NOT NULL,
    category VARCHAR(100) NOT NULL, -- TOLL, DRIVER_BATA, PARKING, VEHICLE_REPAIR, INSURANCE, OFFICE
    vehicle_id BIGINT REFERENCES vehicles(id) ON DELETE SET NULL,
    driver_id BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    trip_id BIGINT REFERENCES trips(id) ON DELETE SET NULL,
    description TEXT,
    amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    gst_amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    total_amount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'CASH' NOT NULL, -- CASH, UPI, BANK_TRANSFER, CHEQUE, CREDIT
    status VARCHAR(50) DEFAULT 'SUBMITTED' NOT NULL, -- DRAFT, SUBMITTED, APPROVED, REJECTED, PAID, CANCELLED
    remarks TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_expense_vehicle ON expenses(vehicle_id);
CREATE INDEX idx_expense_driver ON expenses(driver_id);
CREATE INDEX idx_expense_trip ON expenses(trip_id);
