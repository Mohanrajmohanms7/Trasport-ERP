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
