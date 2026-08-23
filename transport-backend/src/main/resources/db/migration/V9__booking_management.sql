-- Flyway Migration Script: V9__booking_management.sql
-- Creates database schemas for booking headers and itemized details tables

-- 1. Booking Headers Table
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_number VARCHAR(100) NOT NULL UNIQUE,
    booking_date DATE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    delivery_site_id BIGINT REFERENCES customer_delivery_sites(id) ON DELETE SET NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL, -- DRAFT, PENDING, APPROVED, REJECTED, ON_HOLD
    priority VARCHAR(50) DEFAULT 'MEDIUM' NOT NULL, -- HIGH, MEDIUM, LOW
    remarks TEXT,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_booking_customer ON bookings(customer_id);

-- 2. Booking Details Table (Multiple materials per booking)
CREATE TABLE booking_details (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    material_id BIGINT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    quantity NUMERIC(12, 2) NOT NULL,
    rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    transport_rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    royalty_rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    loading_charge NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    gst_percentage NUMERIC(5, 2) DEFAULT 18.00 NOT NULL,
    net_amount NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    description TEXT,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_booking_detail_booking ON booking_details(booking_id);
CREATE INDEX idx_booking_detail_material ON booking_details(material_id);
