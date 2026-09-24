-- Flyway Migration Script: V60__inventory_stock_receipt.sql
-- Phase 11: RECEIPT as the controlled post-opening stock increase.
-- Quantity/audit only. Does not alter opening-balance, issue, return, or accounting.

ALTER TABLE inventory_transactions
    ADD COLUMN supplier_id BIGINT REFERENCES suppliers (id) ON DELETE RESTRICT,
    ADD COLUMN unit_rate NUMERIC(14, 2),
    ADD COLUMN external_reference VARCHAR(100);

ALTER TABLE inventory_transactions
    ADD CONSTRAINT chk_inventory_tx_unit_rate CHECK (unit_rate IS NULL OR unit_rate >= 0);

ALTER TABLE inventory_transactions DROP CONSTRAINT chk_inventory_tx_type;

ALTER TABLE inventory_transactions
    ADD CONSTRAINT chk_inventory_tx_type
        CHECK (transaction_type IN ('OPENING_BALANCE', 'RECEIPT', 'ISSUE', 'RETURN'));

ALTER TABLE inventory_transactions DROP CONSTRAINT chk_inventory_tx_wo_refs;

ALTER TABLE inventory_transactions
    ADD CONSTRAINT chk_inventory_tx_wo_refs CHECK (
        (transaction_type IN ('OPENING_BALANCE', 'RECEIPT')
            AND work_order_id IS NULL
            AND work_order_part_id IS NULL)
        OR
        (transaction_type IN ('ISSUE', 'RETURN')
            AND work_order_id IS NOT NULL
            AND work_order_part_id IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_inventory_tx_company_receipt_ext_ref
    ON inventory_transactions (company_id, lower(external_reference))
    WHERE is_deleted = false
      AND transaction_type = 'RECEIPT'
      AND external_reference IS NOT NULL;

CREATE INDEX idx_inventory_tx_supplier
    ON inventory_transactions (supplier_id)
    WHERE supplier_id IS NOT NULL AND is_deleted = false;
