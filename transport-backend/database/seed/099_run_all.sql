-- 099_run_all.sql — execute after 000_reset_all.sql
\echo 'Run each file in order with psql -f ...'
-- \i 001_company.sql
-- \i 002_branch.sql
-- \i 003_permissions_roles.sql
-- \i 004_users.sql
-- \i 005_lookups.sql
-- \i 006_financial_year_settings.sql
-- \i 010_materials.sql
-- \i 011_loading_locations_quarries.sql
-- \i 012_vehicles.sql
-- \i 013_drivers.sql
-- \i 014_customers.sql
-- \i 020_chart_of_accounts.sql
-- \i 030_bookings_trips.sql
-- \i 040_fuel_expenses.sql
-- \i 045_invoices_payments.sql
-- \i 048_maintenance_reports.sql
-- \i 050_sequences.sql

-- Convenience: if using psql from database/seed directory:
\i 001_company.sql
\i 002_branch.sql
\i 003_permissions_roles.sql
\i 004_users.sql
\i 005_lookups.sql
\i 006_financial_year_settings.sql
\i 010_materials.sql
\i 011_loading_locations_quarries.sql
\i 012_vehicles.sql
\i 013_drivers.sql
\i 014_customers.sql
\i 020_chart_of_accounts.sql
\i 030_bookings_trips.sql
\i 040_fuel_expenses.sql
\i 045_invoices_payments.sql
\i 048_maintenance_reports.sql
\i 050_sequences.sql
