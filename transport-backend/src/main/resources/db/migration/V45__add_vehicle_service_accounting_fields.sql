-- Migration V45: Add accounting fields to vehicle_services table for Step 9B

ALTER TABLE vehicle_services
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    ADD COLUMN IF NOT EXISTS supplier_id BIGINT,
    ADD COLUMN IF NOT EXISTS reference_number VARCHAR(100);

-- Foreign Key constraint to suppliers table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_vehicle_services_supplier'
    ) THEN
        ALTER TABLE vehicle_services
            ADD CONSTRAINT fk_vehicle_services_supplier
            FOREIGN KEY (supplier_id) REFERENCES suppliers(id);
    END IF;
END $$;

-- Populate reference_number for existing records if null
UPDATE vehicle_services
SET reference_number = 'SRV-' || id
WHERE reference_number IS NULL OR reference_number = '';

-- Performance and lookup indexes
CREATE INDEX IF NOT EXISTS idx_vehicle_services_company_status ON vehicle_services(company_id, status);
CREATE INDEX IF NOT EXISTS idx_vehicle_services_ref ON vehicle_services(reference_number);
CREATE INDEX IF NOT EXISTS idx_vehicle_services_supplier ON vehicle_services(supplier_id);
