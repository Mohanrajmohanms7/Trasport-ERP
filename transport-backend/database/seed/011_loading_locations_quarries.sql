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
