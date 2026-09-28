-- V73: one supplier bill often has several parts. The same bill number may be used once per spare part
-- (still blocks entering the same bill line twice).
DROP INDEX IF EXISTS uk_inventory_tx_company_receipt_ext_ref;
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_tx_company_receipt_ext_ref_part
    ON inventory_transactions (company_id, lower(external_reference), spare_part_id)
    WHERE is_deleted = false
      AND transaction_type = 'RECEIPT'
      AND external_reference IS NOT NULL;
