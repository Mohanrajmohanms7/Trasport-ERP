-- 030_bookings_trips.sql — 50 complete ops scenarios
-- Booking -> Vehicle/Driver -> Trip -> Trip details (3 materials each = 150)

INSERT INTO bookings (
 id, booking_number, booking_date, customer_id, delivery_site_id,
 status, priority, remarks, description, company_id, branch_id, code, name, created_by
)
SELECT
  g AS id,
  'BKG' || TO_CHAR(DATE '2025-10-01' + ((g-1) % 90), 'YYMMDD') || LPAD(g::text, 3, '0') AS booking_number,
  DATE '2025-10-01' + ((g-1) % 90) AS booking_date,
  ((g-1) % 25) + 1 AS customer_id,
  ((g-1) % 25) + 1 AS delivery_site_id,
  CASE WHEN g <= 40 THEN 'COMPLETED' WHEN g <= 45 THEN 'IN_PROGRESS' ELSE 'APPROVED' END AS status,
  CASE WHEN g % 5 = 0 THEN 'HIGH' WHEN g % 3 = 0 THEN 'LOW' ELSE 'MEDIUM' END AS priority,
  'Customer call booking — AKS Perambalur ops' AS remarks,
  'Construction material transport booking' AS description,
  1, 1,
  'BKG' || LPAD(g::text, 6, '0'),
  'Booking #' || g,
  'SYSTEM'
FROM generate_series(1, 50) g;

INSERT INTO booking_details (
 id, booking_id, material_id, quantity, rate, transport_rate, royalty_rate, loading_charge,
 gst_percentage, net_amount, status, company_id, branch_id, code, name, created_by
)
SELECT
  ((b.id-1)*2 + m.ord) AS id,
  b.id,
  m.material_id,
  m.qty,
  mat.default_rate,
  450.00,
  35.00,
  120.00,
  18.00,
  ROUND((m.qty * (mat.default_rate + 450 + 35 + 120) * 1.18)::numeric, 2),
  'ACTIVE', 1, 1,
  'BD' || LPAD((((b.id-1)*2 + m.ord))::text, 6, '0'),
  'Booking detail',
  'SYSTEM'
FROM bookings b
CROSS JOIN LATERAL (
  VALUES
    (1, ((b.id-1) % 8) + 1, 12.00 + (b.id % 5)),
    (2, ((b.id + 2) % 8) + 1, 10.00 + (b.id % 4))
) AS m(ord, material_id, qty)
JOIN materials mat ON mat.id = m.material_id;

INSERT INTO trips (
 id, trip_number, trip_date, booking_id, vehicle_id, driver_id,
 status, remarks, description, company_id, branch_id, code, name, created_by
)
SELECT
  g AS id,
  'TRP' || TO_CHAR(DATE '2025-10-01' + ((g-1) % 90), 'YYMMDD') || LPAD(g::text, 3, '0'),
  DATE '2025-10-01' + ((g-1) % 90),
  g,
  ((g-1) % 5) + 1,               -- tippers 1-5 (not JCB for haul trips)
  ((g-1) % 5) + 1,
  CASE WHEN g <= 40 THEN 'COMPLETED' WHEN g <= 45 THEN 'IN_TRANSIT' ELSE 'ALLOCATED' END,
  'Loading from external quarry → customer site delivery',
  'AKS tipper trip',
  1, 1,
  'TRP' || LPAD(g::text, 6, '0'),
  'Trip #' || g,
  'SYSTEM'
FROM generate_series(1, 50) g;

-- 150 trip details (3 per trip)
INSERT INTO trip_details (
 id, trip_id, material_id, quantity, rate, loading_charges, royalty,
 dispatch_time, arrival_time, status, company_id, branch_id, code, name, created_by
)
SELECT
  ((t.id-1)*3 + d.ord) AS id,
  t.id,
  d.material_id,
  d.qty,
  mat.default_rate,
  120.00,
  35.00,
  (t.trip_date::timestamp + TIME '07:30') + ((d.ord-1) || ' hours')::interval,
  (t.trip_date::timestamp + TIME '11:00') + ((d.ord-1) || ' hours')::interval,
  'ACTIVE', 1, 1,
  'TD' || LPAD((((t.id-1)*3 + d.ord))::text, 6, '0'),
  'Trip detail',
  'SYSTEM'
FROM trips t
CROSS JOIN LATERAL (
  VALUES
    (1, ((t.id-1) % 8) + 1, 8.00 + (t.id % 3)),
    (2, ((t.id) % 8) + 1, 7.00 + (t.id % 2)),
    (3, ((t.id+3) % 8) + 1, 6.00 + (t.id % 4))
) AS d(ord, material_id, qty)
JOIN materials mat ON mat.id = d.material_id;
