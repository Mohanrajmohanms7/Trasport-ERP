-- 002_branch.sql — Head Office Perambalur
INSERT INTO branches (
  id, code, name, description, status, company_id,
  gst_number, manager, phone, email, address, latitude, longitude, created_by
) VALUES (
  1, 'HO', 'Head Office', 'AKS Transport Head Office — Perambalur', 'ACTIVE', 1,
  '33AAKCA1234A1Z5', 'K. Selvam', '+91-9751234502', 'ho@akstransport.in',
  'Thannirpandhal, Perambalur, Tamil Nadu - 621212', 11.23450000, 78.88020000, 'SYSTEM'
);
