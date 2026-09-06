-- Step 12B: Vehicle Driver Assignment & Fleet Pairing Lifecycle Integrity

-- 1. Unique constraint: One active assignment per vehicle (where removal_date IS NULL AND is_deleted = false)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'uk_vda_active_vehicle' AND n.nspname = 'public'
    ) THEN
        CREATE UNIQUE INDEX uk_vda_active_vehicle 
        ON vehicle_driver_assignments (vehicle_id) 
        WHERE removal_date IS NULL AND is_deleted = false;
    END IF;
END $$;

-- 2. Unique constraint: One active vehicle per driver (where removal_date IS NULL AND is_deleted = false)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'uk_vda_active_driver' AND n.nspname = 'public'
    ) THEN
        CREATE UNIQUE INDEX uk_vda_active_driver 
        ON vehicle_driver_assignments (driver_id) 
        WHERE removal_date IS NULL AND is_deleted = false;
    END IF;
END $$;

-- 3. Additional performance indexes
CREATE INDEX IF NOT EXISTS idx_vda_vehicle_active ON vehicle_driver_assignments(vehicle_id, removal_date) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_vda_driver_active ON vehicle_driver_assignments(driver_id, removal_date) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_vda_company_branch ON vehicle_driver_assignments(company_id, branch_id) WHERE is_deleted = false;
