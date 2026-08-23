-- 040_fuel_expenses.sql — 100 fuel entries + 60 expenses
INSERT INTO fuel_entries (
 id, fuel_entry_number, fuel_date, vehicle_id, driver_id, trip_id,
 fuel_station, fuel_quantity, rate_per_litre, total_amount, payment_method,
 invoice_number, current_odometer, previous_odometer, remarks, status,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  'FUEL' || LPAD(g::text, 6, '0'),
  DATE '2025-10-01' + ((g-1) % 100),
  ((g-1) % 5) + 1,
  ((g-1) % 5) + 1,
  CASE WHEN g <= 50 THEN g ELSE ((g-1) % 50) + 1 END,
  CASE WHEN g % 3 = 0 THEN 'IOCL Perambalur Bypass'
       WHEN g % 3 = 1 THEN 'HP Thannirpandhal Bunk'
       ELSE 'BPCL Kunnam Road' END,
  40 + (g % 30),
  92.50,
  ROUND(((40 + (g % 30)) * 92.50)::numeric, 2),
  CASE WHEN g % 4 = 0 THEN 'CASH' WHEN g % 4 = 1 THEN 'UPI' ELSE 'CREDIT' END,
  'FS-INV-' || g,
  120000 + (g * 85),
  120000 + ((g-1) * 85),
  'Diesel fill for tipper trip',
  'ACTIVE', 1, 1,
  'FUEL' || LPAD(g::text, 6, '0'),
  'Fuel Entry #' || g,
  'SYSTEM'
FROM generate_series(1, 100) g;

INSERT INTO expenses (
 id, expense_number, expense_date, category, vehicle_id, driver_id, trip_id,
 description, amount, gst_amount, total_amount, payment_method, status, remarks,
 company_id, branch_id, code, name, created_by
)
SELECT
  g,
  'EXP' || LPAD(g::text, 6, '0'),
  DATE '2025-10-01' + ((g-1) % 90),
  CASE (g % 6)
    WHEN 0 THEN 'DRIVER_BATA'
    WHEN 1 THEN 'TOLL'
    WHEN 2 THEN 'PARKING'
    WHEN 3 THEN 'LOADING'
    WHEN 4 THEN 'VEHICLE_REPAIR'
    ELSE 'MISCELLANEOUS' END,
  ((g-1) % 5) + 1,
  ((g-1) % 5) + 1,
  CASE WHEN g <= 50 THEN g ELSE ((g-1) % 50) + 1 END,
  'Trip related operating expense',
  CASE (g % 6) WHEN 0 THEN 800 WHEN 1 THEN 350 WHEN 2 THEN 100 WHEN 3 THEN 500 WHEN 4 THEN 2500 ELSE 200 END,
  0,
  CASE (g % 6) WHEN 0 THEN 800 WHEN 1 THEN 350 WHEN 2 THEN 100 WHEN 3 THEN 500 WHEN 4 THEN 2500 ELSE 200 END,
  CASE WHEN g % 2 = 0 THEN 'CASH' ELSE 'UPI' END,
  CASE WHEN g <= 50 THEN 'APPROVED' ELSE 'SUBMITTED' END,
  'AKS Perambalur ops expense',
  1, 1,
  'EXP' || LPAD(g::text, 6, '0'),
  'Expense #' || g,
  'SYSTEM'
FROM generate_series(1, 60) g;
