-- V88: wheel-count based daily pay slabs (Part 1).
--   vehicles.wheel_count: 6 / 10 / 12 / 14 / 16 / 18 / 22 / EQUIPMENT (optional; blank = not set).
--   driver_pay_slabs.wheel_count: NULL = "All vehicles" (every existing slab), else slabs for that wheel count only.
--   driver_payroll_days.wheel_count: wheel count whose slab paid that day (highest-paying type driven that day).
--   driver_payrolls.excluded_trip_*: trips left out of daily pay because they were already paid per trip (Driver Bata).
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS wheel_count VARCHAR(20);
ALTER TABLE driver_pay_slabs ADD COLUMN IF NOT EXISTS wheel_count VARCHAR(20);
ALTER TABLE driver_payroll_days ADD COLUMN IF NOT EXISTS wheel_count VARCHAR(20);
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS excluded_trip_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE driver_payrolls ADD COLUMN IF NOT EXISTS excluded_trip_numbers TEXT;
