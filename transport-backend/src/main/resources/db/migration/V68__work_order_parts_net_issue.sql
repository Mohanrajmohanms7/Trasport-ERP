-- V68: a returned part can be issued again. The limit is on what is still out (issued - returned),
-- not on the gross issued total.
ALTER TABLE work_order_parts DROP CONSTRAINT IF EXISTS chk_work_order_parts_issued_lte_requested;
ALTER TABLE work_order_parts
    ADD CONSTRAINT chk_work_order_parts_net_issued_lte_requested CHECK (issued_quantity - returned_quantity <= quantity);
