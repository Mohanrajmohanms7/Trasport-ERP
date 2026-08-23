-- Flyway Migration Script: V20__fix_all_missing_base_entity_columns.sql
-- Adds missing BaseEntity columns to all detail/child tables in the system

-- 1. booking_details
ALTER TABLE booking_details ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE booking_details ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE booking_details ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE booking_details ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE booking_details ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE booking_details ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 2. customer_contacts
ALTER TABLE customer_contacts ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE customer_contacts ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE customer_contacts ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE customer_contacts ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE customer_contacts ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE customer_contacts ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 3. customer_delivery_sites
ALTER TABLE customer_delivery_sites ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE customer_delivery_sites ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE customer_delivery_sites ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE customer_delivery_sites ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE customer_delivery_sites ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE customer_delivery_sites ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 4. customer_documents
ALTER TABLE customer_documents ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE customer_documents ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE customer_documents ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE customer_documents ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE customer_documents ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE customer_documents ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 5. driver_attendance
ALTER TABLE driver_attendance ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE driver_attendance ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE driver_attendance ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE driver_attendance ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE driver_attendance ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE driver_attendance ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 6. driver_documents
ALTER TABLE driver_documents ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE driver_documents ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE driver_documents ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE driver_documents ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE driver_documents ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE driver_documents ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 7. driver_salaries
ALTER TABLE driver_salaries ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE driver_salaries ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE driver_salaries ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE driver_salaries ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE driver_salaries ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE driver_salaries ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 8. loading_locations
ALTER TABLE loading_locations ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE loading_locations ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE loading_locations ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE loading_locations ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE loading_locations ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE loading_locations ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 9. material_prices
ALTER TABLE material_prices ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE material_prices ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE material_prices ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE material_prices ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE material_prices ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE material_prices ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 10. sales_invoice_details
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 11. trip_details
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 12. vehicle_documents
ALTER TABLE vehicle_documents ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE vehicle_documents ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE vehicle_documents ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE vehicle_documents ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE vehicle_documents ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE vehicle_documents ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 13. vehicle_driver_assignments
ALTER TABLE vehicle_driver_assignments ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE vehicle_driver_assignments ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE vehicle_driver_assignments ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE vehicle_driver_assignments ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE vehicle_driver_assignments ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE vehicle_driver_assignments ADD COLUMN IF NOT EXISTS branch_id BIGINT;

-- 14. vehicle_services
ALTER TABLE vehicle_services ADD COLUMN IF NOT EXISTS code VARCHAR(50);
ALTER TABLE vehicle_services ADD COLUMN IF NOT EXISTS name VARCHAR(150);
ALTER TABLE vehicle_services ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE vehicle_services ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE vehicle_services ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE vehicle_services ADD COLUMN IF NOT EXISTS branch_id BIGINT;
