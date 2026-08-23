-- Seed dynamic lookup values and settings keys to replace hardcoded strings
-- 1. App Settings (Prefixes, default statuses, etc.)
INSERT INTO app_settings (key_name, value_data, company_id, branch_id, description, is_deleted, version, code, name, status, created_by)
VALUES
  ('PREFIX_BOOKING', 'BKG-', 1, 1, 'Auto-generation prefix for customer bookings', FALSE, 0, 'PREFIX_BOOKING', 'Booking Number Prefix', 'ACTIVE', 'SYSTEM'),
  ('PREFIX_TRIP', 'TRP-', 1, 1, 'Auto-generation prefix for trips', FALSE, 0, 'PREFIX_TRIP', 'Trip Number Prefix', 'ACTIVE', 'SYSTEM'),
  ('PREFIX_INVOICE', 'INV-', 1, 1, 'Auto-generation prefix for sales invoices', FALSE, 0, 'PREFIX_INVOICE', 'Invoice Number Prefix', 'ACTIVE', 'SYSTEM'),
  ('PREFIX_FUEL', 'FUEL-', 1, 1, 'Auto-generation prefix for fuel logs', FALSE, 0, 'PREFIX_FUEL', 'Fuel Entry Prefix', 'ACTIVE', 'SYSTEM'),
  ('PREFIX_EXPENSE', 'EXP-', 1, 1, 'Auto-generation prefix for expenses', FALSE, 0, 'PREFIX_EXPENSE', 'Expense Prefix', 'ACTIVE', 'SYSTEM'),
  ('PREFIX_RECEIPT', 'RCT-', 1, 1, 'Auto-generation prefix for customer payment receipts', FALSE, 0, 'PREFIX_RECEIPT', 'Receipt Prefix', 'ACTIVE', 'SYSTEM'),
  ('DEFAULT_BOOKING_STATUS', 'PENDING', 1, 1, 'Initial workflow status for bookings', FALSE, 0, 'DEFAULT_BOOKING_STATUS', 'Default Booking Status', 'ACTIVE', 'SYSTEM'),
  ('DEFAULT_TRIP_STATUS', 'PLANNED', 1, 1, 'Initial workflow status for trips', FALSE, 0, 'DEFAULT_TRIP_STATUS', 'Default Trip Status', 'ACTIVE', 'SYSTEM'),
  ('DEFAULT_INVOICE_STATUS', 'DRAFT', 1, 1, 'Initial workflow status for invoices', FALSE, 0, 'DEFAULT_INVOICE_STATUS', 'Default Invoice Status', 'ACTIVE', 'SYSTEM'),
  ('DEFAULT_EXPENSE_STATUS', 'SUBMITTED', 1, 1, 'Initial workflow status for expenses', FALSE, 0, 'DEFAULT_EXPENSE_STATUS', 'Default Expense Status', 'ACTIVE', 'SYSTEM')
ON CONFLICT (key_name) DO UPDATE SET value_data = EXCLUDED.value_data;

-- 2. Lookup Values (Priority, Payment Terms, Document types, etc.)
INSERT INTO lookup_values (type, code, name, status, company_id, is_deleted, version, created_by)
VALUES
  ('PRIORITY', 'HIGH', 'High', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('PRIORITY', 'MEDIUM', 'Medium', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('PRIORITY', 'LOW', 'Low', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('PAYMENT_TERMS', 'IMMEDIATE', 'Due on Receipt', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('PAYMENT_TERMS', 'NET_15', 'Net 15 Days', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('PAYMENT_TERMS', 'NET_30', 'Net 30 Days', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('VEHICLE_DOCUMENT_TYPE', 'INSURANCE', 'Insurance', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('VEHICLE_DOCUMENT_TYPE', 'PERMIT', 'Permit', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('VEHICLE_DOCUMENT_TYPE', 'FITNESS', 'Fitness', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('VEHICLE_DOCUMENT_TYPE', 'PUC', 'PUC (Pollution)', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('VEHICLE_DOCUMENT_TYPE', 'ROAD_TAX', 'Road Tax', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('MAINTENANCE_TYPE', 'OIL_CHANGE', 'Oil Change', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('MAINTENANCE_TYPE', 'ENGINE_SERVICE', 'Engine Service', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('MAINTENANCE_TYPE', 'TYRE_CHANGE', 'Tyre Change', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('MAINTENANCE_TYPE', 'BRAKE_SERVICE', 'Brake Service', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('MAINTENANCE_TYPE', 'GENERAL_SERVICE', 'General Maintenance', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('DRIVER_DOCUMENT_TYPE', 'LICENSE_SCAN', 'Driving License', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('DRIVER_DOCUMENT_TYPE', 'AADHAAR', 'Aadhaar Card', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('DRIVER_DOCUMENT_TYPE', 'PAN', 'PAN Card', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('DRIVER_DOCUMENT_TYPE', 'VISION_CERT', 'Vision Certificate', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ATTENDANCE_STATUS', 'PRESENT', 'Present', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ATTENDANCE_STATUS', 'ABSENT', 'Absent', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ATTENDANCE_STATUS', 'LEAVE', 'Leave', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ATTENDANCE_STATUS', 'HALF_DAY', 'Half Day', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('OWNER_TYPE', 'SELF', 'Self Owned', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('OWNER_TYPE', 'HIRED', 'Hired Contractor', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('OWNER_TYPE', 'CLIENT', 'Client Owned', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('STATUS', 'ACTIVE', 'Active', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('STATUS', 'INACTIVE', 'Inactive', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ACCOUNT_TYPE', 'ASSET', 'Asset', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ACCOUNT_TYPE', 'LIABILITY', 'Liability', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ACCOUNT_TYPE', 'EQUITY', 'Equity', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ACCOUNT_TYPE', 'INCOME', 'Income', 'ACTIVE', 1, FALSE, 0, 'SYSTEM'),
  ('ACCOUNT_TYPE', 'EXPENSE', 'Expense', 'ACTIVE', 1, FALSE, 0, 'SYSTEM')
ON CONFLICT (company_id, type, code) DO NOTHING;
