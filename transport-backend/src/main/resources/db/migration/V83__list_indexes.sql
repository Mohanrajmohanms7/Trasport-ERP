-- V83: indexes for company-level lists and reports that had none (speed as data grows; no data change).
CREATE INDEX IF NOT EXISTS idx_bookings_company_status_date ON bookings (company_id, status, booking_date);
CREATE INDEX IF NOT EXISTS idx_journal_vouchers_company_date ON journal_vouchers (company_id, voucher_date);
CREATE INDEX IF NOT EXISTS idx_fuel_entries_company_date ON fuel_entries (company_id, fuel_date);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_name, entity_id);
