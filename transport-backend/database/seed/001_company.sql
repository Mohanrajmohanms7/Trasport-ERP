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
