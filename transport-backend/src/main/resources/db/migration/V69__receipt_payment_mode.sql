-- V69: how a stock receipt was paid. CREDIT = supplier bill (payable), CASH / BANK = paid on the spot.
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(10);
