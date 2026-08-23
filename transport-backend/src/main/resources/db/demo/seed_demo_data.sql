-- >>> 001_company.sql
-- 001_company.sql — AKS Transport
INSERT INTO companies (
  id, code, name, description, status,
  gst_number, pan_number, phone, email, website,
  address, city, state, country, pincode, created_by
) VALUES (
  1, 'AKS', 'AKS Transport',
  'Construction material transport — Thannirpandhal, Perambalur. Own tipper fleet + JCB. Buys from external quarries; supplies customer sites.',
  'ACTIVE',
  '33AAKCA1234A1Z5', 'AAKCA1234A', '+91-9751234501', 'office@akstransport.in', 'www.akstransport.in',
  'Thannirpandhal Main Road, Near Bypass', 'Perambalur', 'Tamil Nadu', 'India', '621212', 'SYSTEM'
);

-- >>> 002_branch.sql
-- 002_branch.sql — Head Office Perambalur
INSERT INTO branches (
  id, code, name, description, status, company_id,
  gst_number, manager, phone, email, address, latitude, longitude, created_by
) VALUES (
  1, 'HO', 'Head Office', 'AKS Transport Head Office — Perambalur', 'ACTIVE', 1,
  '33AAKCA1234A1Z5', 'K. Selvam', '+91-9751234502', 'ho@akstransport.in',
  'Thannirpandhal, Perambalur, Tamil Nadu - 621212', 11.23450000, 78.88020000, 'SYSTEM'
);

-- >>> 003_permissions_roles.sql
-- 003_permissions_roles.sql
INSERT INTO app_permissions (id, code, name, description, status, created_by) VALUES
 (1,'FULL_ACCESS','Full Access','Unrestricted','ACTIVE','SYSTEM'),
 (2,'VIEW','View','View','ACTIVE','SYSTEM'),
 (3,'CREATE','Create','Create','ACTIVE','SYSTEM'),
 (4,'EDIT','Edit','Edit','ACTIVE','SYSTEM'),
 (5,'DELETE','Delete','Delete','ACTIVE','SYSTEM'),
 (6,'APPROVE','Approve','Approve','ACTIVE','SYSTEM'),
 (7,'REJECT','Reject','Reject','ACTIVE','SYSTEM'),
 (8,'EXPORT','Export','Export','ACTIVE','SYSTEM'),
 (9,'IMPORT','Import','Import','ACTIVE','SYSTEM'),
 (10,'PRINT','Print','Print','ACTIVE','SYSTEM');

INSERT INTO app_roles (id, code, name, description, status, company_id, branch_id, created_by) VALUES
 (1,'SUPER_ADMIN','Super Administrator','Full ERP','ACTIVE',1,1,'SYSTEM'),
 (2,'COMPANY_ADMIN','Company Administrator','Company admin','ACTIVE',1,1,'SYSTEM'),
 (3,'MANAGER','Operations Manager','Ops manager','ACTIVE',1,1,'SYSTEM'),
 (4,'DISPATCHER','Dispatcher','Dispatch','ACTIVE',1,1,'SYSTEM'),
 (5,'OPERATOR','Data Operator','Operator','ACTIVE',1,1,'SYSTEM'),
 (6,'ACCOUNTANT','Accountant','Finance','ACTIVE',1,1,'SYSTEM'),
 (7,'DRIVER','Driver','Driver','ACTIVE',1,1,'SYSTEM'),
 (8,'VIEWER','Viewer','Read-only','ACTIVE',1,1,'SYSTEM');

INSERT INTO role_permissions (role_id, permission_id) VALUES
 (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
 (2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),
 (3,2),(3,3),(3,4),(3,6),(3,7),(3,8),(3,10),
 (4,2),(4,3),(4,4),(4,6),(4,10),
 (5,2),(5,3),(5,4),(5,10),
 (6,2),(6,3),(6,4),(6,6),(6,8),(6,10),
 (7,2),(8,2);

-- >>> 004_users.sql
-- 004_users.sql — BCrypt passwords for Spring Security
INSERT INTO app_users (id, code, name, description, username, password, email, phone, status, company_id, branch_id, created_by) VALUES
 (1,'EMP000001','AKS Super Admin','Super admin','superadmin','$2a$10$Vl/4./c4M1XobrFkf0punuArItZwEn/9WdgrrEULy..MzyVvuiyda','superadmin@akstransport.in','+91-9751234001','ACTIVE',1,1,'SYSTEM'),
 (2,'EMP000002','AKS Company Admin','Company admin','admin','$2a$10$y3k6HDGua0Xkk4qPHXFlve2qjjf6t.tGpljCHEkLzEkDvpSREn8Je','admin@akstransport.in','+91-9751234002','ACTIVE',1,1,'SYSTEM'),
 (3,'EMP000003','AKS Operations Manager','Manager','manager','$2a$10$2cvbCVs4pjcUhKiG/lJnEO7N0EuJmW.o0RYWwzUCjj3v/jEiDUiJ6','manager@akstransport.in','+91-9751234003','ACTIVE',1,1,'SYSTEM'),
 (4,'EMP000004','AKS Operator','Operator','operator','$2a$10$Mb8vfeGa/c9do/1epglbpu2Udp1lNr6RTp7e4xrV3TttD7bXAq7TC','operator@akstransport.in','+91-9751234004','ACTIVE',1,1,'SYSTEM'),
 (5,'EMP000005','AKS Accountant','Accountant','accountant','$2a$10$HZ9a3w.9d1R1C5pSUBQWAu0Gcf4vUExfXlsBqAq2s5aBE8X9sZRNC','accounts@akstransport.in','+91-9751234005','ACTIVE',1,1,'SYSTEM'),
 (6,'EMP000006','Murugan','Driver login','driver1','$2a$10$bqCHxsi/2kq2SRnfd4HCVefxrHNsTBY7Y/8.Ag.Io4OGNtw/QEfCq','driver1@akstransport.in','+91-9876541001','ACTIVE',1,1,'SYSTEM'),
 (7,'EMP000007','AKS Viewer','Viewer','viewer','$2a$10$pRVjA9toniAYSoebDFO/Heve594cgd5zrxLlkEszsN4kdeUzovGt2','viewer@akstransport.in','+91-9751234007','ACTIVE',1,1,'SYSTEM');

INSERT INTO user_roles (user_id, role_id) VALUES
 (1,1),(2,2),(3,3),(4,5),(5,6),(6,7),(7,8);

-- >>> 005_lookups.sql
-- 005_lookups.sql — master dropdowns / statuses / types
INSERT INTO lookup_values (type, code, name, description, status, company_id, created_by) VALUES
 ('BOOKING_STATUS','DRAFT','Draft','Draft','ACTIVE',1,'SYSTEM'),
 ('BOOKING_STATUS','PENDING','Pending','Pending','ACTIVE',1,'SYSTEM'),
 ('BOOKING_STATUS','APPROVED','Approved','Approved','ACTIVE',1,'SYSTEM'),
 ('BOOKING_STATUS','IN_PROGRESS','In Progress','In progress','ACTIVE',1,'SYSTEM'),
 ('BOOKING_STATUS','COMPLETED','Completed','Completed','ACTIVE',1,'SYSTEM'),
 ('BOOKING_STATUS','CANCELLED','Cancelled','Cancelled','ACTIVE',1,'SYSTEM'),
 ('BOOKING_STATUS','ON_HOLD','On Hold','On hold','ACTIVE',1,'SYSTEM'),
 ('BOOKING_STATUS','REJECTED','Rejected','Rejected','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','PLANNED','Planned','Planned','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','ALLOCATED','Allocated','Allocated','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','LOADING','Loading','Loading','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','LOADED','Loaded','Loaded','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','DISPATCHED','Dispatched','Dispatched','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','IN_TRANSIT','In Transit','In transit','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','ARRIVED','Arrived','Arrived','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','UNLOADING','Unloading','Unloading','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','DELIVERED','Delivered','Delivered','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','COMPLETED','Completed','Completed','ACTIVE',1,'SYSTEM'),
 ('TRIP_STATUS','CANCELLED','Cancelled','Cancelled','ACTIVE',1,'SYSTEM'),
 ('INVOICE_STATUS','DRAFT','Draft','Draft','ACTIVE',1,'SYSTEM'),
 ('INVOICE_STATUS','PENDING','Pending','Pending','ACTIVE',1,'SYSTEM'),
 ('INVOICE_STATUS','APPROVED','Approved','Approved','ACTIVE',1,'SYSTEM'),
 ('INVOICE_STATUS','GENERATED','Generated','Generated','ACTIVE',1,'SYSTEM'),
 ('INVOICE_STATUS','PAID','Paid','Paid','ACTIVE',1,'SYSTEM'),
 ('INVOICE_STATUS','CANCELLED','Cancelled','Cancelled','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_STATUS','DRAFT','Draft','Draft','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_STATUS','SUBMITTED','Submitted','Submitted','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_STATUS','APPROVED','Approved','Approved','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_STATUS','REJECTED','Rejected','Rejected','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_STATUS','PAID','Paid','Paid','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_STATUS','CANCELLED','Cancelled','Cancelled','ACTIVE',1,'SYSTEM'),
 ('FUEL_REQUEST_STATUS','PENDING','Pending','Pending','ACTIVE',1,'SYSTEM'),
 ('FUEL_REQUEST_STATUS','APPROVED','Approved','Approved','ACTIVE',1,'SYSTEM'),
 ('FUEL_REQUEST_STATUS','REJECTED','Rejected','Rejected','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_STATUS','AVAILABLE','Available','Available','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_STATUS','ON_TRIP','On Trip','On trip','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_STATUS','UNDER_MAINTENANCE','Under Maintenance','Workshop','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_STATUS','IDLE','Idle','Idle','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_STATUS','INACTIVE','Inactive','Inactive','ACTIVE',1,'SYSTEM'),
 ('DRIVER_STATUS','AVAILABLE','Available','Available','ACTIVE',1,'SYSTEM'),
 ('DRIVER_STATUS','ON_TRIP','On Trip','On trip','ACTIVE',1,'SYSTEM'),
 ('DRIVER_STATUS','ON_LEAVE','On Leave','Leave','ACTIVE',1,'SYSTEM'),
 ('DRIVER_STATUS','INACTIVE','Inactive','Inactive','ACTIVE',1,'SYSTEM'),
 ('STATUS','ACTIVE','Active','Active','ACTIVE',1,'SYSTEM'),
 ('STATUS','INACTIVE','Inactive','Inactive','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_TYPE','TIPPER','Tipper Lorry','Tipper','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_TYPE','JCB','JCB / Excavator','JCB','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_CATEGORY','HEAVY','Heavy Commercial Vehicle','HCV','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_CATEGORY','EQUIPMENT','Construction Equipment','Equipment','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_CAPACITY','16_TON','16 Ton','16T','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_CAPACITY','18_TON','18 Ton','18T','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_CAPACITY','20_TON','20 Ton','20T','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_CAPACITY','JCB_STD','JCB Standard','JCB','ACTIVE',1,'SYSTEM'),
 ('MATERIAL_CATEGORY','SAND','Sand','Sand','ACTIVE',1,'SYSTEM'),
 ('MATERIAL_CATEGORY','AGGREGATE','Aggregate / Jalli','Aggregate','ACTIVE',1,'SYSTEM'),
 ('MATERIAL_CATEGORY','DUST','Crusher Dust','Dust','ACTIVE',1,'SYSTEM'),
 ('MATERIAL_CATEGORY','WMM','Wet Mix / Gravel','WMM','ACTIVE',1,'SYSTEM'),
 ('MATERIAL_UNIT','TON','Ton','MT','ACTIVE',1,'SYSTEM'),
 ('MATERIAL_UNIT','LOAD','Load','Load','ACTIVE',1,'SYSTEM'),
 ('MATERIAL_UNIT','CUBIC_METER','Cubic Meter','Cum','ACTIVE',1,'SYSTEM'),
 ('FUEL_TYPE','DIESEL','Diesel','HSD','ACTIVE',1,'SYSTEM'),
 ('FUEL_TYPE','PETROL','Petrol','Petrol','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','DRIVER_BATA','Driver Bata','Bata','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','TOLL','Toll','Toll','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','PARKING','Parking','Parking','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','VEHICLE_REPAIR','Vehicle Repair','Repair','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','VEHICLE_SERVICE','Vehicle Service','Service','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','TYRE','Tyre','Tyre','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','LOADING','Loading Charges','Loading','ACTIVE',1,'SYSTEM'),
 ('EXPENSE_TYPE','MISCELLANEOUS','Miscellaneous','Misc','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','CASH','Cash','Cash','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','UPI','UPI','UPI','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','NEFT','NEFT','NEFT','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','RTGS','RTGS','RTGS','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','IMPS','IMPS','IMPS','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','BANK_TRANSFER','Bank Transfer','Bank','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','CHEQUE','Cheque','Cheque','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_METHOD','CREDIT','Credit','Credit','ACTIVE',1,'SYSTEM'),
 ('PRIORITY','HIGH','High','High','ACTIVE',1,'SYSTEM'),
 ('PRIORITY','MEDIUM','Medium','Medium','ACTIVE',1,'SYSTEM'),
 ('PRIORITY','LOW','Low','Low','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_TERMS','IMMEDIATE','Due on Receipt','Immediate','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_TERMS','NET_7','Net 7 Days','7d','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_TERMS','NET_15','Net 15 Days','15d','ACTIVE',1,'SYSTEM'),
 ('PAYMENT_TERMS','NET_30','Net 30 Days','30d','ACTIVE',1,'SYSTEM'),
 ('CURRENCY','INR','Indian Rupee','INR','ACTIVE',1,'SYSTEM'),
 ('GST_RATE','GST_0','0%','Nil','ACTIVE',1,'SYSTEM'),
 ('GST_RATE','GST_5','5%','5','ACTIVE',1,'SYSTEM'),
 ('GST_RATE','GST_12','12%','12','ACTIVE',1,'SYSTEM'),
 ('GST_RATE','GST_18','18%','18','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_DOCUMENT_TYPE','INSURANCE','Insurance','Insurance','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_DOCUMENT_TYPE','PERMIT','Permit','Permit','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_DOCUMENT_TYPE','FITNESS','Fitness','Fitness','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_DOCUMENT_TYPE','PUC','PUC','PUC','ACTIVE',1,'SYSTEM'),
 ('VEHICLE_DOCUMENT_TYPE','RC','RC Book','RC','ACTIVE',1,'SYSTEM'),
 ('DRIVER_DOCUMENT_TYPE','LICENSE_SCAN','Driving License','DL','ACTIVE',1,'SYSTEM'),
 ('DRIVER_DOCUMENT_TYPE','AADHAAR','Aadhaar','Aadhaar','ACTIVE',1,'SYSTEM'),
 ('DRIVER_DOCUMENT_TYPE','PAN','PAN','PAN','ACTIVE',1,'SYSTEM'),
 ('CUSTOMER_DOCUMENT_TYPE','GST_CERT','GST Certificate','GST','ACTIVE',1,'SYSTEM'),
 ('CUSTOMER_DOCUMENT_TYPE','PAN_CARD','PAN Card','PAN','ACTIVE',1,'SYSTEM'),
 ('MAINTENANCE_TYPE','OIL_CHANGE','Oil Change','Oil','ACTIVE',1,'SYSTEM'),
 ('MAINTENANCE_TYPE','ENGINE_SERVICE','Engine Service','Engine','ACTIVE',1,'SYSTEM'),
 ('MAINTENANCE_TYPE','TYRE_CHANGE','Tyre Change','Tyre','ACTIVE',1,'SYSTEM'),
 ('MAINTENANCE_TYPE','BRAKE_SERVICE','Brake Service','Brake','ACTIVE',1,'SYSTEM'),
 ('MAINTENANCE_TYPE','GENERAL_SERVICE','General Service','General','ACTIVE',1,'SYSTEM'),
 ('ATTENDANCE_STATUS','PRESENT','Present','Present','ACTIVE',1,'SYSTEM'),
 ('ATTENDANCE_STATUS','ABSENT','Absent','Absent','ACTIVE',1,'SYSTEM'),
 ('ATTENDANCE_STATUS','LEAVE','Leave','Leave','ACTIVE',1,'SYSTEM'),
 ('ATTENDANCE_STATUS','HALF_DAY','Half Day','Half','ACTIVE',1,'SYSTEM'),
 ('OWNER_TYPE','SELF','Self Owned','Owned','ACTIVE',1,'SYSTEM'),
 ('OWNER_TYPE','HIRED','Hired','Hired','ACTIVE',1,'SYSTEM'),
 ('ACCOUNT_TYPE','ASSET','Asset','Asset','ACTIVE',1,'SYSTEM'),
 ('ACCOUNT_TYPE','LIABILITY','Liability','Liability','ACTIVE',1,'SYSTEM'),
 ('ACCOUNT_TYPE','EQUITY','Equity','Equity','ACTIVE',1,'SYSTEM'),
 ('ACCOUNT_TYPE','INCOME','Income','Income','ACTIVE',1,'SYSTEM'),
 ('ACCOUNT_TYPE','EXPENSE','Expense','Expense','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','PERAMBALUR','Perambalur','Local','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','ARIYALUR','Ariyalur','Nearby','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','TRICHY','Trichy','Trichy','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','THURAIYUR','Thuraiyur','Thuraiyur','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','NAMAKKAL','Namakkal','Namakkal','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','SALEM','Salem','Salem','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','KARUR','Karur','Karur','ACTIVE',1,'SYSTEM'),
 ('DESTINATION_CITY','THANJAVUR','Thanjavur','Thanjavur','ACTIVE',1,'SYSTEM'),
 ('NOTIFICATION_TYPE','BOOKING','Booking Alert','Booking','ACTIVE',1,'SYSTEM'),
 ('NOTIFICATION_TYPE','TRIP','Trip Alert','Trip','ACTIVE',1,'SYSTEM'),
 ('NOTIFICATION_TYPE','PAYMENT','Payment Alert','Payment','ACTIVE',1,'SYSTEM'),
 ('NOTIFICATION_TYPE','MAINTENANCE','Maintenance Due','Fleet','ACTIVE',1,'SYSTEM'),
 ('DASHBOARD_CARD','TODAY_TRIPS','Today Trips','KPI','ACTIVE',1,'SYSTEM'),
 ('DASHBOARD_CARD','REVENUE_TODAY','Revenue Today','KPI','ACTIVE',1,'SYSTEM'),
 ('DASHBOARD_CARD','OUTSTANDING','Customer Outstanding','KPI','ACTIVE',1,'SYSTEM'),
 ('DASHBOARD_CARD','FLEET_UTIL','Fleet Utilization','KPI','ACTIVE',1,'SYSTEM'),
 ('MENU_GROUP','MASTERS','Masters','Menu','ACTIVE',1,'SYSTEM'),
 ('MENU_GROUP','OPERATIONS','Operations','Menu','ACTIVE',1,'SYSTEM'),
 ('MENU_GROUP','FINANCE','Finance','Menu','ACTIVE',1,'SYSTEM'),
 ('MENU_GROUP','ADMIN','Admin','Menu','ACTIVE',1,'SYSTEM'),
 ('REPORT_TYPE','FLEET','Fleet Report','Report','ACTIVE',1,'SYSTEM'),
 ('REPORT_TYPE','REVENUE','Revenue Report','Report','ACTIVE',1,'SYSTEM'),
 ('REPORT_TYPE','EXPENSE','Expense Report','Report','ACTIVE',1,'SYSTEM'),
 ('REPORT_TYPE','TRIP','Trip Report','Report','ACTIVE',1,'SYSTEM'),
 ('REPORT_TYPE','FUEL','Fuel Report','Report','ACTIVE',1,'SYSTEM')
ON CONFLICT (company_id, type, code) DO NOTHING;

-- >>> 006_financial_year_settings.sql
-- 006_financial_year_settings.sql — FY + app settings (prefixes, GST, currency, menus)
INSERT INTO financial_years (id, code, name, start_date, end_date, is_default, status, company_id, branch_id, description, created_by)
VALUES (1, 'FY2025-26', 'FY 2025-26', '2025-04-01', '2026-03-31', TRUE, 'ACTIVE', 1, 1, 'Indian financial year', 'SYSTEM');

INSERT INTO app_settings (key_name, value_data, code, name, description, status, company_id, branch_id, created_by) VALUES
 ('SETUP_COMPLETED','true','SETUP_COMPLETED','Setup Completed','Wizard finished','ACTIVE',1,1,'SYSTEM'),
 ('COMPANY_DISPLAY_NAME','AKS Transport','COMPANY_DISPLAY_NAME','Company Display Name','UI brand','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_CURRENCY','INR','DEFAULT_CURRENCY','Default Currency','From CURRENCY lookup','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_GST_PERCENT','18','DEFAULT_GST_PERCENT','Default GST %','From GST_RATE lookup','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_STATE','Tamil Nadu','DEFAULT_STATE','Default State','TN','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_COUNTRY','India','DEFAULT_COUNTRY','Default Country','IN','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_VEHICLE','VEH','PREFIX_VEHICLE','Vehicle Prefix','Vehicle codes','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_DRIVER','DRV','PREFIX_DRIVER','Driver Prefix','Driver codes','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_CUSTOMER','CUS','PREFIX_CUSTOMER','Customer Prefix','Customer codes','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_BOOKING','BKG','PREFIX_BOOKING','Booking Prefix','Booking numbers','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_TRIP','TRP','PREFIX_TRIP','Trip Prefix','Trip numbers','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_INVOICE','INV','PREFIX_INVOICE','Invoice Prefix','Invoice numbers','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_FUEL','FUEL','PREFIX_FUEL','Fuel Prefix','Fuel entries','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_EXPENSE','EXP','PREFIX_EXPENSE','Expense Prefix','Expense vouchers','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_RECEIPT','RCT','PREFIX_RECEIPT','Payment Prefix','Receipts','ACTIVE',1,1,'SYSTEM'),
 ('PREFIX_MATERIAL','MAT','PREFIX_MATERIAL','Material Prefix','Materials','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_BOOKING_STATUS','PENDING','DEFAULT_BOOKING_STATUS','Default Booking Status','lookup BOOKING_STATUS','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_TRIP_STATUS','PLANNED','DEFAULT_TRIP_STATUS','Default Trip Status','lookup TRIP_STATUS','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_INVOICE_STATUS','DRAFT','DEFAULT_INVOICE_STATUS','Default Invoice Status','lookup INVOICE_STATUS','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_EXPENSE_STATUS','SUBMITTED','DEFAULT_EXPENSE_STATUS','Default Expense Status','lookup EXPENSE_STATUS','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_PAYMENT_METHOD','UPI','DEFAULT_PAYMENT_METHOD','Default Payment Method','lookup PAYMENT_METHOD','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_PAYMENT_TERMS','NET_15','DEFAULT_PAYMENT_TERMS','Default Payment Terms','lookup PAYMENT_TERMS','ACTIVE',1,1,'SYSTEM'),
 ('DEFAULT_PRIORITY','MEDIUM','DEFAULT_PRIORITY','Default Priority','lookup PRIORITY','ACTIVE',1,1,'SYSTEM'),
 ('PRINT_TEMPLATE_INVOICE','AKS_INV_V1','PRINT_TEMPLATE_INVOICE','Invoice Print Template','Template code','ACTIVE',1,1,'SYSTEM'),
 ('PRINT_TEMPLATE_BOOKING','AKS_BKG_V1','PRINT_TEMPLATE_BOOKING','Booking Print Template','Template code','ACTIVE',1,1,'SYSTEM'),
 ('MENU_CONFIG_VERSION','1','MENU_CONFIG_VERSION','Menu Config Version','Sidebar config','ACTIVE',1,1,'SYSTEM'),
 ('REPORT_CONFIG_VERSION','1','REPORT_CONFIG_VERSION','Report Config Version','Reports','ACTIVE',1,1,'SYSTEM')
ON CONFLICT (key_name) DO UPDATE SET value_data = EXCLUDED.value_data, company_id = 1, branch_id = 1;

-- >>> 010_materials.sql
-- 010_materials.sql
INSERT INTO materials (id, code, name, description, category_id, unit_id, default_rate, density, status, company_id, branch_id, created_by) VALUES
 (1,'MAT000001','M Sand','Construction material — M Sand',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='SAND'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),850.00,1.500,'ACTIVE',1,1,'SYSTEM'),
 (2,'MAT000002','P Sand','Construction material — P Sand',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='SAND'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),780.00,1.500,'ACTIVE',1,1,'SYSTEM'),
 (3,'MAT000003','20 MM Jalli','Construction material — 20 MM Jalli',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='AGGREGATE'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),920.00,1.500,'ACTIVE',1,1,'SYSTEM'),
 (4,'MAT000004','40 MM Jalli','Construction material — 40 MM Jalli',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='AGGREGATE'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),880.00,1.500,'ACTIVE',1,1,'SYSTEM'),
 (5,'MAT000005','Blue Metal','Construction material — Blue Metal',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='AGGREGATE'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),950.00,1.500,'ACTIVE',1,1,'SYSTEM'),
 (6,'MAT000006','Crusher Dust','Construction material — Crusher Dust',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='DUST'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),420.00,1.500,'ACTIVE',1,1,'SYSTEM'),
 (7,'MAT000007','Wet Mix','Construction material — Wet Mix',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='WMM'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),1100.00,1.500,'ACTIVE',1,1,'SYSTEM'),
 (8,'MAT000008','Gravel','Construction material — Gravel',(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_CATEGORY' AND code='WMM'),(SELECT id FROM lookup_values WHERE company_id=1 AND type='MATERIAL_UNIT' AND code='TON'),650.00,1.500,'ACTIVE',1,1,'SYSTEM');


INSERT INTO material_prices (id, material_id, material_rate, transport_rate, royalty_rate, loading_charge, effective_date, status, company_id, branch_id, code, name, created_by)
SELECT m.id, m.id, m.default_rate, 450.00, 35.00, 120.00, '2025-04-01', 'ACTIVE', 1, 1,
       'MP' || LPAD(m.id::text, 6, '0'), m.name || ' Price', 'SYSTEM'
FROM materials m WHERE m.company_id = 1;

-- >>> 011_loading_locations_quarries.sql
-- 011_loading_locations_quarries.sql — external quarries / crushers (AKS has no own quarry)
INSERT INTO quarries (id, code, name, description, location_address, owner_name, contact_number, gst_number, latitude, longitude, status, company_id, branch_id, created_by) VALUES
 (1,'QRY000001','Sri Lakshmi Blue Metals','External crusher','Thannirpandhal Road, Perambalur','R. Lakshmanan','+91-98430-11001','33AABCL1111A1Z1',11.24100000,78.87500000,'ACTIVE',1,1,'SYSTEM'),
 (2,'QRY000002','Sakthi Crushers','External crusher','Kunnam Main Road','S. Sakthivel','+91-98430-11002','33AABCS2222A1Z2',11.19800000,78.91000000,'ACTIVE',1,1,'SYSTEM'),
 (3,'QRY000003','Raja Blue Metals','External quarry','Padalur Bypass','M. Raja','+91-98430-11003','33AABCR3333A1Z3',11.17500000,78.86000000,'ACTIVE',1,1,'SYSTEM'),
 (4,'QRY000004','Sri Balaji Quarry','External quarry','Veppanthattai Road','B. Balaji','+91-98430-11004','33AABCB4444A1Z4',11.26000000,78.92000000,'ACTIVE',1,1,'SYSTEM'),
 (5,'QRY000005','Perambalur Blue Metals','External crusher','NH-136 Perambalur','P. Murugesan','+91-98430-11005','33AABCP5555A1Z5',11.23000000,78.88500000,'ACTIVE',1,1,'SYSTEM');

INSERT INTO loading_locations (id, location_code, loading_point, loading_charges, latitude, longitude, status, company_id, branch_id, code, name, description, created_by) VALUES
 (1,'LOC000001','Sri Lakshmi Blue Metals Yard',120.00,11.24100000,78.87500000,'ACTIVE',1,1,'LOC000001','Sri Lakshmi Yard','Loading point','SYSTEM'),
 (2,'LOC000002','Sakthi Crushers Gate',130.00,11.19800000,78.91000000,'ACTIVE',1,1,'LOC000002','Sakthi Crushers','Loading point','SYSTEM'),
 (3,'LOC000003','Raja Blue Metals Bay',125.00,11.17500000,78.86000000,'ACTIVE',1,1,'LOC000003','Raja Blue Metals','Loading point','SYSTEM'),
 (4,'LOC000004','Sri Balaji Quarry Ramp',140.00,11.26000000,78.92000000,'ACTIVE',1,1,'LOC000004','Sri Balaji Quarry','Loading point','SYSTEM'),
 (5,'LOC000005','Perambalur Blue Metals Dock',115.00,11.23000000,78.88500000,'ACTIVE',1,1,'LOC000005','PBM Dock','Loading point','SYSTEM'),
 (6,'LOC000006','Alathur Aggregate Point',110.00,11.15000000,78.84000000,'ACTIVE',1,1,'LOC000006','Alathur Point','Loading point','SYSTEM'),
 (7,'LOC000007','Thuraiyur Road Crusher',135.00,11.32000000,78.80000000,'ACTIVE',1,1,'LOC000007','Thuraiyur Crusher','Loading point','SYSTEM');

-- >>> 012_vehicles.sql
-- 012_vehicles.sql — 5 tipper lorries + 1 JCB (TN registrations)
INSERT INTO vehicles (
 id, code, name, description, chassis_number, engine_number, model, brand,
 type_id, category_id, capacity_id, owner_name, owner_type, purchase_date,
 insurance_expiry_date, fitness_expiry_date, permit_expiry_date,
 status, company_id, branch_id, created_by
) VALUES
 (1,'VEH000001','TN 46 AB 1001 Tipper','16T tipper lorry','CHS-AKS-T16-001','ENG-AKS-001','2021','Ashok Leyland',
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_TYPE' AND code='TIPPER'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CATEGORY' AND code='HEAVY'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CAPACITY' AND code='16_TON'),
  'AKS Transport','SELF','2021-06-15','2027-06-14','2027-06-14','2028-06-14','AVAILABLE',1,1,'SYSTEM'),
 (2,'VEH000002','TN 46 AB 1002 Tipper','18T tipper lorry','CHS-AKS-T18-002','ENG-AKS-002','2022','Tata Motors',
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_TYPE' AND code='TIPPER'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CATEGORY' AND code='HEAVY'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CAPACITY' AND code='18_TON'),
  'AKS Transport','SELF','2022-02-20','2027-02-19','2027-02-19','2028-02-19','AVAILABLE',1,1,'SYSTEM'),
 (3,'VEH000003','TN 46 AB 1003 Tipper','20T tipper lorry','CHS-AKS-T20-003','ENG-AKS-003','2022','BharatBenz',
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_TYPE' AND code='TIPPER'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CATEGORY' AND code='HEAVY'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CAPACITY' AND code='20_TON'),
  'AKS Transport','SELF','2022-08-10','2027-08-09','2027-08-09','2028-08-09','AVAILABLE',1,1,'SYSTEM'),
 (4,'VEH000004','TN 46 AB 1004 Tipper','16T tipper lorry','CHS-AKS-T16-004','ENG-AKS-004','2023','Ashok Leyland',
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_TYPE' AND code='TIPPER'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CATEGORY' AND code='HEAVY'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CAPACITY' AND code='16_TON'),
  'AKS Transport','SELF','2023-01-05','2028-01-04','2028-01-04','2029-01-04','AVAILABLE',1,1,'SYSTEM'),
 (5,'VEH000005','TN 46 AB 1005 Tipper','18T tipper lorry','CHS-AKS-T18-005','ENG-AKS-005','2023','Tata Motors',
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_TYPE' AND code='TIPPER'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CATEGORY' AND code='HEAVY'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CAPACITY' AND code='18_TON'),
  'AKS Transport','SELF','2023-09-18','2028-09-17','2028-09-17','2029-09-17','AVAILABLE',1,1,'SYSTEM'),
 (6,'VEH000006','TN 46 JC 2001 JCB','JCB 3DX','CHS-AKS-JCB-001','ENG-AKS-JCB','2020','JCB',
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_TYPE' AND code='JCB'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CATEGORY' AND code='EQUIPMENT'),
  (SELECT id FROM lookup_values WHERE company_id=1 AND type='VEHICLE_CAPACITY' AND code='JCB_STD'),
  'AKS Transport','SELF','2020-11-12','2026-11-11','2026-11-11','2027-11-11','AVAILABLE',1,1,'SYSTEM');

INSERT INTO vehicle_documents (vehicle_id, doc_type, doc_number, expiry_date, file_path, status, company_id, branch_id, code, name, created_by)
SELECT v.id, 'INSURANCE', 'INS-AKS-' || v.id, v.insurance_expiry_date, '/docs/vehicles/' || v.code || '_ins.pdf',
       'ACTIVE',1,1,'VD'||LPAD(v.id::text,5,'0'), v.name || ' Insurance', 'SYSTEM'
FROM vehicles v WHERE v.company_id=1;

-- >>> 013_drivers.sql
-- 013_drivers.sql — 5 Tamil drivers
INSERT INTO drivers (id, code, name, description, license_number, license_expiry_date, phone_number, status, company_id, branch_id, created_by) VALUES
 (1,'DRV000001','Murugan','Tipper driver — Perambalur','TN4620150012345','2030-03-15','+91-9876541001','AVAILABLE',1,1,'SYSTEM'),
 (2,'DRV000002','Rajesh','Tipper driver — Kunnam','TN4620160023456','2031-07-20','+91-9876541002','AVAILABLE',1,1,'SYSTEM'),
 (3,'DRV000003','Suresh','Tipper driver — Padalur','TN4620170034567','2029-11-10','+91-9876541003','AVAILABLE',1,1,'SYSTEM'),
 (4,'DRV000004','Kumar','Tipper driver — Veppanthattai','TN4620180045678','2032-01-25','+91-9876541004','AVAILABLE',1,1,'SYSTEM'),
 (5,'DRV000005','Arun','Tipper / JCB operator','TN4620190056789','2031-05-05','+91-9876541005','AVAILABLE',1,1,'SYSTEM');

INSERT INTO driver_documents (driver_id, doc_type, doc_number, file_path, status, company_id, branch_id, code, name, created_by)
SELECT d.id, 'LICENSE_SCAN', d.license_number, '/docs/drivers/'||d.code||'_dl.pdf','ACTIVE',1,1,'DD'||LPAD(d.id::text,5,'0'), d.name||' License','SYSTEM'
FROM drivers d WHERE d.company_id=1;

INSERT INTO driver_salaries (driver_id, basic_salary, overtime_rate, advance_taken, status, company_id, branch_id, code, name, created_by)
SELECT d.id, 18000 + (d.id*500), 100.00, CASE WHEN d.id%2=0 THEN 2000 ELSE 0 END, 'ACTIVE',1,1,
       'SAL'||LPAD(d.id::text,5,'0'), d.name||' Salary','SYSTEM'
FROM drivers d WHERE d.company_id=1;

INSERT INTO vehicle_driver_assignments (vehicle_id, driver_id, assignment_date, removal_date, status, company_id, branch_id, code, name, created_by) VALUES
 (1,1,'2025-04-01',NULL,'ACTIVE',1,1,'VDA00001','VEH000001-Murugan','SYSTEM'),
 (2,2,'2025-04-01',NULL,'ACTIVE',1,1,'VDA00002','VEH000002-Rajesh','SYSTEM'),
 (3,3,'2025-04-01',NULL,'ACTIVE',1,1,'VDA00003','VEH000003-Suresh','SYSTEM'),
 (4,4,'2025-04-01',NULL,'ACTIVE',1,1,'VDA00004','VEH000004-Kumar','SYSTEM'),
 (5,5,'2025-04-01',NULL,'ACTIVE',1,1,'VDA00005','VEH000005-Arun','SYSTEM');

-- >>> 014_customers.sql
-- 014_customers.sql — 25 construction customers around Perambalur belt
INSERT INTO customers (id, code, name, description, email, phone, address, gst_number, credit_limit, status, company_id, branch_id, created_by) VALUES
 (1,'CUS000001','Sri Murugan Constructions','Construction customer — Perambalur','accounts1@customer-aks.in','+91-9870000111','Thuraimangalam, Perambalur, Tamil Nadu - 621212','33AAK01C0001A1Z2',500000.00,'ACTIVE',1,1,'SYSTEM'),
 (2,'CUS000002','Sakthi Builders','Construction customer — Perambalur','accounts2@customer-aks.in','+91-9870000222','Elambalur, Perambalur, Tamil Nadu - 621220','33AAK02C0002A1Z3',350000.00,'ACTIVE',1,1,'SYSTEM'),
 (3,'CUS000003','Annai Infra Projects','Construction customer — Perambalur','accounts3@customer-aks.in','+91-9870000333','Padalur, Perambalur, Tamil Nadu - 621108','33AAK03C0003A1Z4',750000.00,'ACTIVE',1,1,'SYSTEM'),
 (4,'CUS000004','KVS Ready Mix','Construction customer — Perambalur','accounts4@customer-aks.in','+91-9870000444','Kunnam, Perambalur, Tamil Nadu - 621108','33AAK04C0004A1Z5',400000.00,'ACTIVE',1,1,'SYSTEM'),
 (5,'CUS000005','Raja Housing','Construction customer — Perambalur','accounts5@customer-aks.in','+91-9870000555','Veppanthattai, Perambalur, Tamil Nadu - 621116','33AAK05C0005A1Z6',300000.00,'ACTIVE',1,1,'SYSTEM'),
 (6,'CUS000006','Thangam Concrete Works','Construction customer — Ariyalur','accounts6@customer-aks.in','+91-9870000666','Ariyalur Town, Ariyalur, Tamil Nadu - 621704','33AAK06C0006A1Z7',600000.00,'ACTIVE',1,1,'SYSTEM'),
 (7,'CUS000007','Lakshmi Blue Metal Traders','Construction customer — Ariyalur','accounts7@customer-aks.in','+91-9870000777','Jayankondam, Ariyalur, Tamil Nadu - 621802','33AAK07C0007A1Z8',450000.00,'ACTIVE',1,1,'SYSTEM'),
 (8,'CUS000008','Pooja Infra','Construction customer — Ariyalur','accounts8@customer-aks.in','+91-9870000888','Sendurai, Ariyalur, Tamil Nadu - 621714','33AAK08C0008A1Z9',280000.00,'ACTIVE',1,1,'SYSTEM'),
 (9,'CUS000009','Trichy City Builders','Construction customer — Trichy','accounts9@customer-aks.in','+91-9870000999','Thillai Nagar, Trichy, Tamil Nadu - 620018','33AAK09C0009A1Z1',900000.00,'ACTIVE',1,1,'SYSTEM'),
 (10,'CUS000010','Cauvery Constructions','Construction customer — Trichy','accounts10@customer-aks.in','+91-9870001110','Srirangam, Trichy, Tamil Nadu - 620006','33AAK10C0010A1Z2',550000.00,'ACTIVE',1,1,'SYSTEM'),
 (11,'CUS000011','Rockford Projects','Construction customer — Trichy','accounts11@customer-aks.in','+91-9870001221','Thuvakudi, Trichy, Tamil Nadu - 620015','33AAK11C0011A1Z3',700000.00,'ACTIVE',1,1,'SYSTEM'),
 (12,'CUS000012','Delta Earth Movers','Construction customer — Trichy','accounts12@customer-aks.in','+91-9870001332','Samayapuram, Trichy, Tamil Nadu - 621112','33AAK12C0012A1Z4',320000.00,'ACTIVE',1,1,'SYSTEM'),
 (13,'CUS000013','Sri Amman Builders','Construction customer — Thuraiyur','accounts13@customer-aks.in','+91-9870001443','Thuraiyur Bus Stand, Thuraiyur, Tamil Nadu - 621010','33AAK13C0013A1Z5',250000.00,'ACTIVE',1,1,'SYSTEM'),
 (14,'CUS000014','Green Valley Homes','Construction customer — Thuraiyur','accounts14@customer-aks.in','+91-9870001554','Uppiliyapuram, Thuraiyur, Tamil Nadu - 621008','33AAK14C0014A1Z6',220000.00,'ACTIVE',1,1,'SYSTEM'),
 (15,'CUS000015','Vetri Sand Suppliers','Construction customer — Veppanthattai','accounts15@customer-aks.in','+91-9870001665','Poolambadi, Veppanthattai, Tamil Nadu - 621116','33AAK15C0015A1Z7',180000.00,'ACTIVE',1,1,'SYSTEM'),
 (16,'CUS000016','Arul Jothi Contractors','Construction customer — Kunnam','accounts16@customer-aks.in','+91-9870001776','Labbaikudikadu, Kunnam, Tamil Nadu - 621104','33AAK16C0016A1Z8',260000.00,'ACTIVE',1,1,'SYSTEM'),
 (17,'CUS000017','Padalur Township Works','Construction customer — Padalur','accounts17@customer-aks.in','+91-9870001887','Padalur Main, Padalur, Tamil Nadu - 621108','33AAK17C0017A1Z9',310000.00,'ACTIVE',1,1,'SYSTEM'),
 (18,'CUS000018','Alathur Gramam Panchayat Works','Construction customer — Alathur','accounts18@customer-aks.in','+91-9870001998','Alathur, Alathur, Tamil Nadu - 621109','33AAK18C0018A1Z1',150000.00,'ACTIVE',1,1,'SYSTEM'),
 (19,'CUS000019','Namakkal Steel Structures','Construction customer — Namakkal','accounts19@customer-aks.in','+91-9870002109','Tiruchengode Road, Namakkal, Tamil Nadu - 637001','33AAK19C0019A1Z2',480000.00,'ACTIVE',1,1,'SYSTEM'),
 (20,'CUS000020','Salem Aggregates Corp','Construction customer — Salem','accounts20@customer-aks.in','+91-9870002220','Attur Road, Salem, Tamil Nadu - 636007','33AAK20C0020A1Z3',650000.00,'ACTIVE',1,1,'SYSTEM'),
 (21,'CUS000021','Karur Textile Park Infra','Construction customer — Karur','accounts21@customer-aks.in','+91-9870002331','Thanthonimalai, Karur, Tamil Nadu - 639005','33AAK21C0021A1Z4',520000.00,'ACTIVE',1,1,'SYSTEM'),
 (22,'CUS000022','Thanjavur Temple Town Builders','Construction customer — Thanjavur','accounts22@customer-aks.in','+91-9870002442','Medical College Road, Thanjavur, Tamil Nadu - 613004','33AAK22C0022A1Z5',410000.00,'ACTIVE',1,1,'SYSTEM'),
 (23,'CUS000023','Sri Balaji Site Developers','Construction customer — Perambalur','accounts23@customer-aks.in','+91-9870002553','Esanai, Perambalur, Tamil Nadu - 621212','33AAK23C0023A1Z6',290000.00,'ACTIVE',1,1,'SYSTEM'),
 (24,'CUS000024','OM Sakthi Earth Works','Construction customer — Ariyalur','accounts24@customer-aks.in','+91-9870002664','Andimadam, Ariyalur, Tamil Nadu - 621801','33AAK24C0024A1Z7',240000.00,'ACTIVE',1,1,'SYSTEM'),
 (25,'CUS000025','Trichy Bypass Road Contractors','Construction customer — Trichy','accounts25@customer-aks.in','+91-9870002775','No.1 Toll Plaza Area, Trichy, Tamil Nadu - 620012','33AAK25C0025A1Z8',800000.00,'ACTIVE',1,1,'SYSTEM');


INSERT INTO customer_contacts (customer_id, contact_name, designation, email, phone, status, company_id, branch_id, code, name, created_by)
SELECT c.id, split_part(c.name,' ',1) || ' Manager', 'Site Manager', c.email, c.phone, 'ACTIVE',1,1,
       'CC'||LPAD(c.id::text,5,'0'), c.name||' Contact','SYSTEM'
FROM customers c WHERE c.company_id=1;

INSERT INTO customer_delivery_sites (id, customer_id, site_code, site_name, address, manager_name, status, company_id, branch_id, code, name, created_by)
SELECT c.id, c.id, 'SITE'||LPAD(c.id::text,4,'0'), c.name||' Site', c.address, 'Site Incharge', 'ACTIVE',1,1,
       'SITE'||LPAD(c.id::text,4,'0'), c.name||' Delivery Site','SYSTEM'
FROM customers c WHERE c.company_id=1;

-- >>> 020_chart_of_accounts.sql
-- 020_chart_of_accounts.sql
INSERT INTO chart_of_accounts (id, account_code, account_name, account_type, opening_balance, running_balance, code, name, status, company_id, branch_id, description, created_by) VALUES
 (1,'1000','Cash on Hand','ASSET',50000,50000,'1000','Cash on Hand','ACTIVE',1,1,'Cash','SYSTEM'),
 (2,'1010','Bank - Current A/c','ASSET',350000,350000,'1010','Bank Current','ACTIVE',1,1,'Bank','SYSTEM'),
 (3,'1100','Customer Receivables','ASSET',0,0,'1100','Receivables','ACTIVE',1,1,'AR','SYSTEM'),
 (4,'2000','Supplier Payables','LIABILITY',0,0,'2000','Payables','ACTIVE',1,1,'AP','SYSTEM'),
 (5,'3000','Owner Capital','EQUITY',400000,400000,'3000','Capital','ACTIVE',1,1,'Equity','SYSTEM'),
 (6,'4000','Transport Freight Income','INCOME',0,0,'4000','Freight Income','ACTIVE',1,1,'Income','SYSTEM'),
 (7,'5000','Fuel Expense','EXPENSE',0,0,'5000','Fuel Expense','ACTIVE',1,1,'Expense','SYSTEM'),
 (8,'5100','Driver Bata & Trip Expense','EXPENSE',0,0,'5100','Trip Expense','ACTIVE',1,1,'Expense','SYSTEM'),
 (9,'5200','Vehicle Maintenance','EXPENSE',0,0,'5200','Maintenance','ACTIVE',1,1,'Expense','SYSTEM'),
 (10,'5300','Toll & Parking','EXPENSE',0,0,'5300','Toll Parking','ACTIVE',1,1,'Expense','SYSTEM');

-- >>> 030_bookings_trips.sql
-- 030_bookings_trips.sql — 50 complete ops scenarios
-- Booking -> Vehicle/Driver -> Trip -> Trip details (3 materials each = 150)

INSERT INTO bookings (
 id, booking_number, booking_date, customer_id, delivery_site_id,
 status, priority, remarks, description, company_id, branch_id, code, name, created_by
)
SELECT
  g AS id,
  'BKG' || TO_CHAR(DATE '2025-10-01' + ((g-1) % 90), 'YYMMDD') || LPAD(g::text, 3, '0') AS booking_number,
  DATE '2025-10-01' + ((g-1) % 90) AS booking_date,
  ((g-1) % 25) + 1 AS customer_id,
  ((g-1) % 25) + 1 AS delivery_site_id,
  CASE WHEN g <= 40 THEN 'COMPLETED' WHEN g <= 45 THEN 'IN_PROGRESS' ELSE 'APPROVED' END AS status,
  CASE WHEN g % 5 = 0 THEN 'HIGH' WHEN g % 3 = 0 THEN 'LOW' ELSE 'MEDIUM' END AS priority,
  'Customer call booking — AKS Perambalur ops' AS remarks,
  'Construction material transport booking' AS description,
  1, 1,
  'BKG' || LPAD(g::text, 6, '0'),
  'Booking #' || g,
  'SYSTEM'
FROM generate_series(1, 50) g;

INSERT INTO booking_details (
 id, booking_id, material_id, quantity, rate, transport_rate, royalty_rate, loading_charge,
 gst_percentage, net_amount, status, company_id, branch_id, code, name, created_by
)
SELECT
  ((b.id-1)*2 + m.ord) AS id,
  b.id,
  m.material_id,
  m.qty,
  mat.default_rate,
  450.00,
  35.00,
  120.00,
  18.00,
  ROUND((m.qty * (mat.default_rate + 450 + 35 + 120) * 1.18)::numeric, 2),
  'ACTIVE', 1, 1,
  'BD' || LPAD((((b.id-1)*2 + m.ord))::text, 6, '0'),
  'Booking detail',
  'SYSTEM'
FROM bookings b
CROSS JOIN LATERAL (
  VALUES
    (1, ((b.id-1) % 8) + 1, 12.00 + (b.id % 5)),
    (2, ((b.id + 2) % 8) + 1, 10.00 + (b.id % 4))
) AS m(ord, material_id, qty)
JOIN materials mat ON mat.id = m.material_id;

INSERT INTO trips (
 id, trip_number, trip_date, booking_id, vehicle_id, driver_id,
 status, remarks, description, company_id, branch_id, code, name, created_by
)
SELECT
  g AS id,
  'TRP' || TO_CHAR(DATE '2025-10-01' + ((g-1) % 90), 'YYMMDD') || LPAD(g::text, 3, '0'),
  DATE '2025-10-01' + ((g-1) % 90),
  g,
  ((g-1) % 5) + 1,               -- tippers 1-5 (not JCB for haul trips)
  ((g-1) % 5) + 1,
  CASE WHEN g <= 40 THEN 'COMPLETED' WHEN g <= 45 THEN 'IN_TRANSIT' ELSE 'ALLOCATED' END,
  'Loading from external quarry → customer site delivery',
  'AKS tipper trip',
  1, 1,
  'TRP' || LPAD(g::text, 6, '0'),
  'Trip #' || g,
  'SYSTEM'
FROM generate_series(1, 50) g;

-- 150 trip details (3 per trip)
INSERT INTO trip_details (
 id, trip_id, material_id, quantity, rate, loading_charges, royalty,
 dispatch_time, arrival_time, status, company_id, branch_id, code, name, created_by
)
SELECT
  ((t.id-1)*3 + d.ord) AS id,
  t.id,
  d.material_id,
  d.qty,
  mat.default_rate,
  120.00,
  35.00,
  (t.trip_date::timestamp + TIME '07:30') + ((d.ord-1) || ' hours')::interval,
  (t.trip_date::timestamp + TIME '11:00') + ((d.ord-1) || ' hours')::interval,
  'ACTIVE', 1, 1,
  'TD' || LPAD((((t.id-1)*3 + d.ord))::text, 6, '0'),
  'Trip detail',
  'SYSTEM'
FROM trips t
CROSS JOIN LATERAL (
  VALUES
    (1, ((t.id-1) % 8) + 1, 8.00 + (t.id % 3)),
    (2, ((t.id) % 8) + 1, 7.00 + (t.id % 2)),
    (3, ((t.id+3) % 8) + 1, 6.00 + (t.id % 4))
) AS d(ord, material_id, qty)
JOIN materials mat ON mat.id = d.material_id;

-- >>> 040_fuel_expenses.sql
-- 040_fuel_expenses.sql — 100 fuel entries + 60 expenses
INSERT INTO fuel_entries (
 id, fuel_entry_number, fuel_date, vehicle_id, driver_id, trip_id,
 fuel_station, fuel_quantity, rate_per_litre, total_amount, payment_method,
 invoice_number, current_odometer, previous_odometer, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  'FUEL' || LPAD(g::text, 6, '0'),
  DATE '2025-10-01' + ((g-1) % 100),
  ((g-1) % 5) + 1,
  ((g-1) % 5) + 1,
  CASE WHEN g <= 50 THEN g ELSE ((g-1) % 50) + 1 END,
  CASE WHEN g % 3 = 0 THEN 'IOCL Perambalur Bypass'
       WHEN g % 3 = 1 THEN 'HP Thannirpandhal Bunk'
       ELSE 'BPCL Kunnam Road' END,
  40 + (g % 30),
  92.50,
  ROUND(((40 + (g % 30)) * 92.50)::numeric, 2),
  CASE WHEN g % 4 = 0 THEN 'CASH' WHEN g % 4 = 1 THEN 'UPI' ELSE 'CREDIT' END,
  'FS-INV-' || g,
  120000 + (g * 85),
  120000 + ((g-1) * 85),
  'Diesel fill for tipper trip',
  'ACTIVE', 1, 1,
  'FUEL' || LPAD(g::text, 6, '0'),
  'Fuel Entry #' || g,
  'SYSTEM'
FROM generate_series(1, 100) g;

INSERT INTO expenses (
 id, expense_number, expense_date, category, vehicle_id, driver_id, trip_id,
 description, amount, gst_amount, total_amount, payment_method, status, remarks,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  'EXP' || LPAD(g::text, 6, '0'),
  DATE '2025-10-01' + ((g-1) % 90),
  CASE (g % 6)
    WHEN 0 THEN 'DRIVER_BATA'
    WHEN 1 THEN 'TOLL'
    WHEN 2 THEN 'PARKING'
    WHEN 3 THEN 'LOADING'
    WHEN 4 THEN 'VEHICLE_REPAIR'
    ELSE 'MISCELLANEOUS' END,
  ((g-1) % 5) + 1,
  ((g-1) % 5) + 1,
  CASE WHEN g <= 50 THEN g ELSE ((g-1) % 50) + 1 END,
  'Trip related operating expense',
  CASE (g % 6) WHEN 0 THEN 800 WHEN 1 THEN 350 WHEN 2 THEN 100 WHEN 3 THEN 500 WHEN 4 THEN 2500 ELSE 200 END,
  0,
  CASE (g % 6) WHEN 0 THEN 800 WHEN 1 THEN 350 WHEN 2 THEN 100 WHEN 3 THEN 500 WHEN 4 THEN 2500 ELSE 200 END,
  CASE WHEN g % 2 = 0 THEN 'CASH' ELSE 'UPI' END,
  CASE WHEN g <= 50 THEN 'APPROVED' ELSE 'SUBMITTED' END,
  'AKS Perambalur ops expense',
  1, 1,
  'EXP' || LPAD(g::text, 6, '0'),
  'Expense #' || g,
  'SYSTEM'
FROM generate_series(1, 60) g;

-- >>> 045_invoices_payments.sql
-- 045_invoices_payments.sql — 30 invoices + details, 40 payments, ledger
INSERT INTO sales_invoices (
 id, invoice_number, invoice_date, customer_id, status, payment_terms,
 subtotal, discount, net_amount, company_id, branch_id, code, name, description, created_by
)
SELECT
  g,
  'INV' || TO_CHAR(DATE '2025-11-01' + (g-1), 'YYMM') || LPAD(g::text, 4, '0'),
  DATE '2025-11-01' + ((g-1) * 2),
  ((g-1) % 25) + 1,
  CASE WHEN g <= 20 THEN 'GENERATED' WHEN g <= 25 THEN 'APPROVED' ELSE 'DRAFT' END,
  'NET_15',
  45000 + (g * 1500),
  0,
  ROUND(((45000 + (g * 1500)) * 1.18)::numeric, 2),
  1, 1,
  'INV' || LPAD(g::text, 6, '0'),
  'Invoice #' || g,
  'Freight invoice for completed trips',
  'SYSTEM'
FROM generate_series(1, 30) g;

INSERT INTO sales_invoice_details (
 id, invoice_id, trip_id, material_id, quantity, rate, freight_charges, loading_charges, royalty,
 gst_percentage, cgst, sgst, igst, net_amount, status, company_id, branch_id, code, name, created_by
)
SELECT
  g,
  ((g-1) % 30) + 1,
  ((g-1) % 50) + 1,
  ((g-1) % 8) + 1,
  12 + (g % 5),
  m.default_rate,
  450,
  120,
  35,
  18,
  ROUND((((12 + (g % 5)) * (m.default_rate + 450 + 120 + 35) * 0.09))::numeric, 2),
  ROUND((((12 + (g % 5)) * (m.default_rate + 450 + 120 + 35) * 0.09))::numeric, 2),
  0,
  ROUND((((12 + (g % 5)) * (m.default_rate + 450 + 120 + 35) * 1.18))::numeric, 2),
  'ACTIVE', 1, 1,
  'ID' || LPAD(g::text, 6, '0'),
  'Invoice line',
  'SYSTEM'
FROM generate_series(1, 60) g
JOIN materials m ON m.id = ((g-1) % 8) + 1;

INSERT INTO customer_receipts (
 id, receipt_number, receipt_date, customer_id, booking_id,
 amount_received, advance_amount, payment_method, reference_number, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  'RCT' || LPAD(g::text, 6, '0'),
  DATE '2025-11-05' + g,
  ((g-1) % 25) + 1,
  CASE WHEN g <= 40 THEN ((g-1) % 50) + 1 ELSE NULL END,
  25000 + (g * 800),
  CASE WHEN g % 5 = 0 THEN 5000 ELSE 0 END,
  CASE WHEN g % 3 = 0 THEN 'NEFT' WHEN g % 3 = 1 THEN 'UPI' ELSE 'CASH' END,
  'UTR-AKS-' || (100000 + g),
  'Payment against transport invoice / booking',
  'ACTIVE', 1, 1,
  'RCT' || LPAD(g::text, 6, '0'),
  'Receipt #' || g,
  'SYSTEM'
FROM generate_series(1, 40) g;

-- Ledger: invoice debits (30) + payment credits (40)
INSERT INTO customer_ledgers (
 id, customer_id, receipt_id, debit_amount, credit_amount, running_balance, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  i.customer_id,
  NULL,
  i.net_amount,
  0,
  i.net_amount,
  'Invoice posting ' || i.invoice_number,
  'ACTIVE', 1, 1,
  'CLD' || LPAD(g::text, 6, '0'),
  'Debit ' || i.invoice_number,
  'SYSTEM'
FROM generate_series(1, 30) g
JOIN sales_invoices i ON i.id = g;

INSERT INTO customer_ledgers (
 id, customer_id, receipt_id, debit_amount, credit_amount, running_balance, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  30 + g,
  r.customer_id,
  r.id,
  0,
  r.amount_received,
  GREATEST(0, (25000 + (g * 200)) - r.amount_received),
  'Payment posting ' || r.receipt_number,
  'ACTIVE', 1, 1,
  'CLC' || LPAD(g::text, 6, '0'),
  'Credit ' || r.receipt_number,
  'SYSTEM'
FROM generate_series(1, 40) g
JOIN customer_receipts r ON r.id = g;

INSERT INTO journal_vouchers (
 id, voucher_number, voucher_date, reference_number, description,
 debit_account_id, credit_account_id, amount, status, code, name, company_id, branch_id, created_by
)
SELECT
  g,
  'JV' || LPAD(g::text, 6, '0'),
  DATE '2025-11-01' + g,
  'INV-REF-' || g,
  'Freight income recognition',
  3,
  6,
  45000 + (g * 1000),
  'ACTIVE',
  'JV' || LPAD(g::text, 6, '0'),
  'Journal #' || g,
  1, 1,
  'SYSTEM'
FROM generate_series(1, 20) g;

-- >>> 048_maintenance_reports.sql
-- 048_maintenance_reports.sql
INSERT INTO vehicle_services (
 vehicle_id, service_type, service_date, next_service_date, workshop, cost, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  v.id,
  CASE WHEN v.id % 2 = 0 THEN 'OIL_CHANGE' ELSE 'GENERAL_SERVICE' END,
  DATE '2025-09-01' + ((v.id * 7) || ' days')::interval,
  DATE '2026-03-01' + ((v.id * 7) || ' days')::interval,
  'Perambalur Heavy Motors',
  4500 + (v.id * 300),
  'Scheduled maintenance',
  'ACTIVE', 1, 1,
  'VS' || LPAD(v.id::text, 5, '0'),
  v.name || ' Service',
  'SYSTEM'
FROM vehicles v WHERE v.company_id = 1 AND v.id <= 5;

INSERT INTO report_templates (id, template_name, report_type, columns_list, status, company_id, branch_id, code, name, created_by) VALUES
 (1,'AKS Fleet Daily','FLEET','vehicle,status,trips','ACTIVE',1,1,'RPT0001','Fleet Daily','SYSTEM'),
 (2,'AKS Revenue Summary','REVENUE','invoice,customer,amount','ACTIVE',1,1,'RPT0002','Revenue Summary','SYSTEM'),
 (3,'AKS Trip Register','TRIP','trip,vehicle,driver,destination','ACTIVE',1,1,'RPT0003','Trip Register','SYSTEM'),
 (4,'AKS Fuel Consumption','FUEL','vehicle,litres,amount','ACTIVE',1,1,'RPT0004','Fuel Consumption','SYSTEM'),
 (5,'AKS Expense Register','EXPENSE','category,amount,trip','ACTIVE',1,1,'RPT0005','Expense Register','SYSTEM');

INSERT INTO audit_logs (username, action, entity_name, entity_id, action_time, ip_address, details)
VALUES ('admin', 'AKS_SEED_COMPLETE', 'companies', 1, NOW(), '127.0.0.1',
        'AKS Transport Perambalur seed loaded — masters, 50 bookings/trips, fuel, expenses, invoices, payments');

-- >>> 050_sequences.sql
-- 050_sequences.sql — align serials after explicit IDs
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT c.relname AS seq, t.relname AS tbl
    FROM pg_class c
    JOIN pg_depend d ON d.objid = c.oid
    JOIN pg_class t ON d.refobjid = t.oid
    WHERE c.relkind = 'S' AND t.relnamespace = 'public'::regnamespace
  LOOP
    EXECUTE format(
      'SELECT setval(%L, COALESCE((SELECT MAX(id) FROM %I), 1), true)',
      r.seq, r.tbl
    );
  END LOOP;
EXCEPTION WHEN OTHERS THEN
  -- fallback common tables
  PERFORM setval(pg_get_serial_sequence('companies','id'), COALESCE((SELECT MAX(id) FROM companies),1), true);
END $$;

SELECT setval(pg_get_serial_sequence('companies','id'), COALESCE((SELECT MAX(id) FROM companies),1), true);
SELECT setval(pg_get_serial_sequence('branches','id'), COALESCE((SELECT MAX(id) FROM branches),1), true);
SELECT setval(pg_get_serial_sequence('app_users','id'), COALESCE((SELECT MAX(id) FROM app_users),1), true);
SELECT setval(pg_get_serial_sequence('app_roles','id'), COALESCE((SELECT MAX(id) FROM app_roles),1), true);
SELECT setval(pg_get_serial_sequence('app_permissions','id'), COALESCE((SELECT MAX(id) FROM app_permissions),1), true);
SELECT setval(pg_get_serial_sequence('vehicles','id'), COALESCE((SELECT MAX(id) FROM vehicles),1), true);
SELECT setval(pg_get_serial_sequence('drivers','id'), COALESCE((SELECT MAX(id) FROM drivers),1), true);
SELECT setval(pg_get_serial_sequence('customers','id'), COALESCE((SELECT MAX(id) FROM customers),1), true);
SELECT setval(pg_get_serial_sequence('materials','id'), COALESCE((SELECT MAX(id) FROM materials),1), true);
SELECT setval(pg_get_serial_sequence('bookings','id'), COALESCE((SELECT MAX(id) FROM bookings),1), true);
SELECT setval(pg_get_serial_sequence('trips','id'), COALESCE((SELECT MAX(id) FROM trips),1), true);
SELECT setval(pg_get_serial_sequence('fuel_entries','id'), COALESCE((SELECT MAX(id) FROM fuel_entries),1), true);
SELECT setval(pg_get_serial_sequence('expenses','id'), COALESCE((SELECT MAX(id) FROM expenses),1), true);
SELECT setval(pg_get_serial_sequence('sales_invoices','id'), COALESCE((SELECT MAX(id) FROM sales_invoices),1), true);
SELECT setval(pg_get_serial_sequence('customer_receipts','id'), COALESCE((SELECT MAX(id) FROM customer_receipts),1), true);
SELECT setval(pg_get_serial_sequence('customer_ledgers','id'), COALESCE((SELECT MAX(id) FROM customer_ledgers),1), true);

