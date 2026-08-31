ALTER TABLE customer_receipts
ADD COLUMN approved_by VARCHAR(100),
ADD COLUMN approved_at TIMESTAMP;
