-- Tenant-scope remaining document numbers / codes across ops & finance modules.
-- Same pattern as V36: drop global UNIQUE, add (company_id, code/number) uniqueness.

-- Loading locations
ALTER TABLE loading_locations DROP CONSTRAINT IF EXISTS loading_locations_location_code_key;
DROP INDEX IF EXISTS loading_locations_location_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_loading_locations_company_code
    ON loading_locations (company_id, location_code)
    WHERE is_deleted = false;

-- Bookings
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_booking_number_key;
DROP INDEX IF EXISTS bookings_booking_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_bookings_company_number
    ON bookings (company_id, booking_number)
    WHERE is_deleted = false;

-- Trips
ALTER TABLE trips DROP CONSTRAINT IF EXISTS trips_trip_number_key;
DROP INDEX IF EXISTS trips_trip_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_trips_company_number
    ON trips (company_id, trip_number)
    WHERE is_deleted = false;

-- Fuel entries / requests
ALTER TABLE fuel_entries DROP CONSTRAINT IF EXISTS fuel_entries_fuel_entry_number_key;
DROP INDEX IF EXISTS fuel_entries_fuel_entry_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_fuel_entries_company_number
    ON fuel_entries (company_id, fuel_entry_number)
    WHERE is_deleted = false;

ALTER TABLE fuel_requests DROP CONSTRAINT IF EXISTS fuel_requests_request_number_key;
DROP INDEX IF EXISTS fuel_requests_request_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_fuel_requests_company_number
    ON fuel_requests (company_id, request_number)
    WHERE is_deleted = false;

-- Expenses
ALTER TABLE expenses DROP CONSTRAINT IF EXISTS expenses_expense_number_key;
DROP INDEX IF EXISTS expenses_expense_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_expenses_company_number
    ON expenses (company_id, expense_number)
    WHERE is_deleted = false;

-- Customer receipts
ALTER TABLE customer_receipts DROP CONSTRAINT IF EXISTS customer_receipts_receipt_number_key;
DROP INDEX IF EXISTS customer_receipts_receipt_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_receipts_company_number
    ON customer_receipts (company_id, receipt_number)
    WHERE is_deleted = false;

-- Sales invoices
ALTER TABLE sales_invoices DROP CONSTRAINT IF EXISTS sales_invoices_invoice_number_key;
DROP INDEX IF EXISTS sales_invoices_invoice_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_sales_invoices_company_number
    ON sales_invoices (company_id, invoice_number)
    WHERE is_deleted = false;

-- Chart of accounts / journal vouchers
ALTER TABLE chart_of_accounts DROP CONSTRAINT IF EXISTS chart_of_accounts_account_code_key;
DROP INDEX IF EXISTS chart_of_accounts_account_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_chart_of_accounts_company_code
    ON chart_of_accounts (company_id, account_code)
    WHERE is_deleted = false;

ALTER TABLE journal_vouchers DROP CONSTRAINT IF EXISTS journal_vouchers_voucher_number_key;
DROP INDEX IF EXISTS journal_vouchers_voucher_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_journal_vouchers_company_number
    ON journal_vouchers (company_id, voucher_number)
    WHERE is_deleted = false;

-- App user employee codes (username stays globally unique for login)
ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_code_key;
DROP INDEX IF EXISTS app_users_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_users_company_code
    ON app_users (company_id, code)
    WHERE is_deleted = false AND code IS NOT NULL;
