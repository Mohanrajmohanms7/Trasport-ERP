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
