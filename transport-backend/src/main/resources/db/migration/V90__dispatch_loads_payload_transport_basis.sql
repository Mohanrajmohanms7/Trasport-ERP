-- V90: one lorry load = one trip.
--   vehicles.payload_capacity / payload_uom_id: how much one load carries (e.g. 2 Unit, 16 Ton); a trip may not exceed it
--   (when the units match). Optional: blank = no payload check.
--   booking_details.transport_basis: PER_UNIT (existing: transport rate x quantity) or PER_TRIP (a fixed / distance-based
--   amount per load, entered on each trip).
--   trip_details.transport_amount / sales_invoice_details.transport_amount: that per-load amount (added once, not x qty).
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS payload_capacity NUMERIC(12,3);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS payload_uom_id BIGINT REFERENCES uom_master(id);
ALTER TABLE booking_details ADD COLUMN IF NOT EXISTS transport_basis VARCHAR(10) NOT NULL DEFAULT 'PER_UNIT';
ALTER TABLE trip_details ADD COLUMN IF NOT EXISTS transport_amount NUMERIC(14,2);
ALTER TABLE sales_invoice_details ADD COLUMN IF NOT EXISTS transport_amount NUMERIC(14,2);
ALTER TABLE booking_details DROP CONSTRAINT IF EXISTS chk_booking_transport_basis;
ALTER TABLE booking_details ADD CONSTRAINT chk_booking_transport_basis CHECK (transport_basis IN ('PER_UNIT', 'PER_TRIP'));
