-- Flyway Migration Script: V39__receipt_cancellation_indexes.sql
-- Optimizes receipt lookups of active vs cancelled receipt documents

CREATE INDEX IF NOT EXISTS idx_receipts_status ON customer_receipts(status);
