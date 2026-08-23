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
