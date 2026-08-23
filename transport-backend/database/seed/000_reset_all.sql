-- ============================================================================
-- 000_reset_all.sql — wipe all ERP tables (keep flyway_schema_history)
-- ============================================================================
SET session_replication_role = 'replica';
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN (
    SELECT tablename FROM pg_tables
    WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
  ) LOOP
    EXECUTE format('TRUNCATE TABLE %I RESTART IDENTITY CASCADE', r.tablename);
  END LOOP;
END $$;
SET session_replication_role = 'origin';
ANALYZE;
