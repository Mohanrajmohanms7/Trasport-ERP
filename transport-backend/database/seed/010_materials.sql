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
