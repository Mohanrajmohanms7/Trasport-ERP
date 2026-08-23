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
