-- 050_sequences.sql — align serials after explicit IDs
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT c.relname AS seq, t.relname AS tbl
    FROM pg_class c
    JOIN pg_depend d ON d.objid = c.oid
    JOIN pg_class t ON d.refobjid = t.oid
    WHERE c.relkind = 'S' AND t.relnamespace = 'public'::regnamespace
  LOOP
    EXECUTE format(
      'SELECT setval(%L, COALESCE((SELECT MAX(id) FROM %I), 1), true)',
      r.seq, r.tbl
    );
  END LOOP;
EXCEPTION WHEN OTHERS THEN
  -- fallback common tables
  PERFORM setval(pg_get_serial_sequence('companies','id'), COALESCE((SELECT MAX(id) FROM companies),1), true);
END $$;

SELECT setval(pg_get_serial_sequence('companies','id'), COALESCE((SELECT MAX(id) FROM companies),1), true);
SELECT setval(pg_get_serial_sequence('branches','id'), COALESCE((SELECT MAX(id) FROM branches),1), true);
SELECT setval(pg_get_serial_sequence('app_users','id'), COALESCE((SELECT MAX(id) FROM app_users),1), true);
SELECT setval(pg_get_serial_sequence('app_roles','id'), COALESCE((SELECT MAX(id) FROM app_roles),1), true);
SELECT setval(pg_get_serial_sequence('app_permissions','id'), COALESCE((SELECT MAX(id) FROM app_permissions),1), true);
SELECT setval(pg_get_serial_sequence('vehicles','id'), COALESCE((SELECT MAX(id) FROM vehicles),1), true);
SELECT setval(pg_get_serial_sequence('drivers','id'), COALESCE((SELECT MAX(id) FROM drivers),1), true);
SELECT setval(pg_get_serial_sequence('customers','id'), COALESCE((SELECT MAX(id) FROM customers),1), true);
SELECT setval(pg_get_serial_sequence('materials','id'), COALESCE((SELECT MAX(id) FROM materials),1), true);
SELECT setval(pg_get_serial_sequence('bookings','id'), COALESCE((SELECT MAX(id) FROM bookings),1), true);
SELECT setval(pg_get_serial_sequence('trips','id'), COALESCE((SELECT MAX(id) FROM trips),1), true);
SELECT setval(pg_get_serial_sequence('fuel_entries','id'), COALESCE((SELECT MAX(id) FROM fuel_entries),1), true);
SELECT setval(pg_get_serial_sequence('expenses','id'), COALESCE((SELECT MAX(id) FROM expenses),1), true);
SELECT setval(pg_get_serial_sequence('sales_invoices','id'), COALESCE((SELECT MAX(id) FROM sales_invoices),1), true);
SELECT setval(pg_get_serial_sequence('customer_receipts','id'), COALESCE((SELECT MAX(id) FROM customer_receipts),1), true);
SELECT setval(pg_get_serial_sequence('customer_ledgers','id'), COALESCE((SELECT MAX(id) FROM customer_ledgers),1), true);
