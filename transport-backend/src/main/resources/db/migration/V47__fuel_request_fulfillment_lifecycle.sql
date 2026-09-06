-- Step 11B: Fuel Request Authorization & Consumption Lifecycle Integrity
ALTER TABLE fuel_entries ADD COLUMN IF NOT EXISTS fuel_request_id BIGINT REFERENCES fuel_requests(id);

ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS fulfilled_quantity NUMERIC(10, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS fulfilled_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE fuel_requests ADD COLUMN IF NOT EXISTS fuel_entry_id BIGINT REFERENCES fuel_entries(id);

-- Enforce exactly-once fulfillment per fuel request
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_fuel_entry_request'
    ) THEN
        ALTER TABLE fuel_entries ADD CONSTRAINT uk_fuel_entry_request UNIQUE (fuel_request_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_fuel_entry_request ON fuel_entries(fuel_request_id);
CREATE INDEX IF NOT EXISTS idx_fuel_request_company_status ON fuel_requests(company_id, status);
