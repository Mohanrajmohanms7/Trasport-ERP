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
