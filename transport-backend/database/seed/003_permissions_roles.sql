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
