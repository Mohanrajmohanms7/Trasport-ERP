-- 045_invoices_payments.sql — 30 invoices + details, 40 payments, ledger
INSERT INTO sales_invoices (
 id, invoice_number, invoice_date, customer_id, status, payment_terms,
 subtotal, discount, net_amount, company_id, branch_id, code, name, description, created_by
)
SELECT
  g,
  'INV' || TO_CHAR(DATE '2025-11-01' + (g-1), 'YYMM') || LPAD(g::text, 4, '0'),
  DATE '2025-11-01' + ((g-1) * 2),
  ((g-1) % 25) + 1,
  CASE WHEN g <= 20 THEN 'GENERATED' WHEN g <= 25 THEN 'APPROVED' ELSE 'DRAFT' END,
  'NET_15',
  45000 + (g * 1500),
  0,
  ROUND(((45000 + (g * 1500)) * 1.18)::numeric, 2),
  1, 1,
  'INV' || LPAD(g::text, 6, '0'),
  'Invoice #' || g,
  'Freight invoice for completed trips',
  'SYSTEM'
FROM generate_series(1, 30) g;

INSERT INTO sales_invoice_details (
 id, invoice_id, trip_id, material_id, quantity, rate, freight_charges, loading_charges, royalty,
 gst_percentage, cgst, sgst, igst, net_amount, status, company_id, branch_id, code, name, created_by
)
SELECT
  g,
  ((g-1) % 30) + 1,
  ((g-1) % 50) + 1,
  ((g-1) % 8) + 1,
  12 + (g % 5),
  m.default_rate,
  450,
  120,
  35,
  18,
  ROUND((((12 + (g % 5)) * (m.default_rate + 450 + 120 + 35) * 0.09))::numeric, 2),
  ROUND((((12 + (g % 5)) * (m.default_rate + 450 + 120 + 35) * 0.09))::numeric, 2),
  0,
  ROUND((((12 + (g % 5)) * (m.default_rate + 450 + 120 + 35) * 1.18))::numeric, 2),
  'ACTIVE', 1, 1,
  'ID' || LPAD(g::text, 6, '0'),
  'Invoice line',
  'SYSTEM'
FROM generate_series(1, 60) g
JOIN materials m ON m.id = ((g-1) % 8) + 1;

INSERT INTO customer_receipts (
 id, receipt_number, receipt_date, customer_id, booking_id,
 amount_received, advance_amount, payment_method, reference_number, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  'RCT' || LPAD(g::text, 6, '0'),
  DATE '2025-11-05' + g,
  ((g-1) % 25) + 1,
  CASE WHEN g <= 40 THEN ((g-1) % 50) + 1 ELSE NULL END,
  25000 + (g * 800),
  CASE WHEN g % 5 = 0 THEN 5000 ELSE 0 END,
  CASE WHEN g % 3 = 0 THEN 'NEFT' WHEN g % 3 = 1 THEN 'UPI' ELSE 'CASH' END,
  'UTR-AKS-' || (100000 + g),
  'Payment against transport invoice / booking',
  'ACTIVE', 1, 1,
  'RCT' || LPAD(g::text, 6, '0'),
  'Receipt #' || g,
  'SYSTEM'
FROM generate_series(1, 40) g;

-- Ledger: invoice debits (30) + payment credits (40)
INSERT INTO customer_ledgers (
 id, customer_id, receipt_id, debit_amount, credit_amount, running_balance, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  i.customer_id,
  NULL,
  i.net_amount,
  0,
  i.net_amount,
  'Invoice posting ' || i.invoice_number,
  'ACTIVE', 1, 1,
  'CLD' || LPAD(g::text, 6, '0'),
  'Debit ' || i.invoice_number,
  'SYSTEM'
FROM generate_series(1, 30) g
JOIN sales_invoices i ON i.id = g;

INSERT INTO customer_ledgers (
 id, customer_id, receipt_id, debit_amount, credit_amount, running_balance, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  30 + g,
  r.customer_id,
  r.id,
  0,
  r.amount_received,
  GREATEST(0, (25000 + (g * 200)) - r.amount_received),
  'Payment posting ' || r.receipt_number,
  'ACTIVE', 1, 1,
  'CLC' || LPAD(g::text, 6, '0'),
  'Credit ' || r.receipt_number,
  'SYSTEM'
FROM generate_series(1, 40) g
JOIN customer_receipts r ON r.id = g;

INSERT INTO journal_vouchers (
 id, voucher_number, voucher_date, reference_number, description,
 debit_account_id, credit_account_id, amount, status, code, name, company_id, branch_id, created_by
)
SELECT
  g,
  'JV' || LPAD(g::text, 6, '0'),
  DATE '2025-11-01' + g,
  'INV-REF-' || g,
  'Freight income recognition',
  3,
  6,
  45000 + (g * 1000),
  'ACTIVE',
  'JV' || LPAD(g::text, 6, '0'),
  'Journal #' || g,
  1, 1,
  'SYSTEM'
FROM generate_series(1, 20) g;
