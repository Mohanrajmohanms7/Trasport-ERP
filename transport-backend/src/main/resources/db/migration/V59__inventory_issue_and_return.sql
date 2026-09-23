-- Flyway Migration Script: V59__inventory_issue_and_return.sql
-- Phase 10: ISSUE/RETURN against warehouse stock and work-order parts.
-- Quantity/audit only. Does not alter opening-balance, accounting, or trip blocking.

ALTER TABLE work_order_parts
    ADD COLUMN issued_quantity NUMERIC(12, 3) NOT NULL DEFAULT 0,
    ADD COLUMN returned_quantity NUMERIC(12, 3) NOT NULL DEFAULT 0;

ALTER TABLE work_order_parts
    ADD CONSTRAINT chk_work_order_parts_issued_qty CHECK (issued_quantity >= 0),
    ADD CONSTRAINT chk_work_order_parts_returned_qty CHECK (returned_quantity >= 0),
    ADD CONSTRAINT chk_work_order_parts_returned_lte_issued CHECK (returned_quantity <= issued_quantity),
    ADD CONSTRAINT chk_work_order_parts_issued_lte_requested CHECK (issued_quantity <= quantity);

ALTER TABLE inventory_transactions
    ADD COLUMN work_order_id BIGINT REFERENCES work_orders (id) ON DELETE RESTRICT,
    ADD COLUMN work_order_part_id BIGINT REFERENCES work_order_parts (id) ON DELETE RESTRICT;

ALTER TABLE inventory_transactions DROP CONSTRAINT chk_inventory_tx_type;

ALTER TABLE inventory_transactions
    ADD CONSTRAINT chk_inventory_tx_type CHECK (transaction_type IN ('OPENING_BALANCE', 'ISSUE', 'RETURN'));

ALTER TABLE inventory_transactions
    ADD CONSTRAINT chk_inventory_tx_wo_refs CHECK (
        (transaction_type = 'OPENING_BALANCE' AND work_order_id IS NULL AND work_order_part_id IS NULL)
        OR
        (transaction_type IN ('ISSUE', 'RETURN') AND work_order_id IS NOT NULL AND work_order_part_id IS NOT NULL)
    );

CREATE INDEX idx_inventory_tx_work_order
    ON inventory_transactions (work_order_id)
    WHERE work_order_id IS NOT NULL AND is_deleted = false;

CREATE INDEX idx_inventory_tx_work_order_part
    ON inventory_transactions (work_order_part_id)
    WHERE work_order_part_id IS NOT NULL AND is_deleted = false;
