-- Tenant-scope master codes: each company can reuse codes like CUS000002.
-- Previously UNIQUE(code) was global, so PKR could not create a customer code
-- that already existed for AKS/DEMO.

-- Customers
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_code_key;
DROP INDEX IF EXISTS customers_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_company_code
    ON customers (company_id, code)
    WHERE is_deleted = false;

-- Vehicles (registration / code)
ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS vehicles_code_key;
DROP INDEX IF EXISTS vehicles_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicles_company_code
    ON vehicles (company_id, code)
    WHERE is_deleted = false;

-- Drivers (employee code)
ALTER TABLE drivers DROP CONSTRAINT IF EXISTS drivers_code_key;
DROP INDEX IF EXISTS drivers_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_drivers_company_code
    ON drivers (company_id, code)
    WHERE is_deleted = false;

-- Driver license: allow same license number only once per company
ALTER TABLE drivers DROP CONSTRAINT IF EXISTS drivers_license_number_key;
DROP INDEX IF EXISTS drivers_license_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_drivers_company_license
    ON drivers (company_id, license_number)
    WHERE is_deleted = false;

-- Suppliers
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS suppliers_code_key;
DROP INDEX IF EXISTS suppliers_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_suppliers_company_code
    ON suppliers (company_id, code)
    WHERE is_deleted = false;

-- Materials
ALTER TABLE materials DROP CONSTRAINT IF EXISTS materials_code_key;
DROP INDEX IF EXISTS materials_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_materials_company_code
    ON materials (company_id, code)
    WHERE is_deleted = false;

-- Quarries
ALTER TABLE quarries DROP CONSTRAINT IF EXISTS quarries_code_key;
DROP INDEX IF EXISTS quarries_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_quarries_company_code
    ON quarries (company_id, code)
    WHERE is_deleted = false;
