-- V67: moving average cost per warehouse stock row, so parts issued to work orders carry a cost
-- and the spare-parts inventory account moves with receipts and issues.
ALTER TABLE warehouse_stock ADD COLUMN IF NOT EXISTS average_cost NUMERIC(14, 4) NOT NULL DEFAULT 0;
-- Seed from the latest priced receipt where one exists (older stock stays at 0 = not valued).
UPDATE warehouse_stock s SET average_cost = x.unit_rate
FROM (
    SELECT DISTINCT ON (warehouse_id, spare_part_id) warehouse_id, spare_part_id, unit_rate
    FROM inventory_transactions
    WHERE is_deleted = FALSE AND transaction_type = 'RECEIPT' AND unit_rate IS NOT NULL AND unit_rate > 0
    ORDER BY warehouse_id, spare_part_id, id DESC
) x
WHERE s.warehouse_id = x.warehouse_id AND s.spare_part_id = x.spare_part_id AND s.average_cost = 0;
