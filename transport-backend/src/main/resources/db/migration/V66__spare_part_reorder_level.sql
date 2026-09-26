-- V66: minimum stock per spare part; stock at or below it is flagged "Reorder", zero is "Out of stock".
ALTER TABLE spare_parts ADD COLUMN IF NOT EXISTS reorder_level NUMERIC(14, 3);
