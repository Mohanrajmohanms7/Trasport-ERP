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
