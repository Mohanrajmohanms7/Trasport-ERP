-- Flyway Migration Script: V56__maintenance_work_order_financial_posting.sql
-- Phase 6: lookup support for the maintenance work-order journal voucher.
-- Posting state is derived from journal_vouchers.reference_number.
-- Does not alter work_orders, parts, labour, expenses, service logs, or inventory.

CREATE INDEX IF NOT EXISTS idx_journal_vouchers_company_reference
    ON journal_vouchers (company_id, reference_number)
    WHERE is_deleted = false;
