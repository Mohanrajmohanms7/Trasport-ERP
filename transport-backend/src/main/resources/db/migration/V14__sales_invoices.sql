-- Flyway Migration Script: V14__sales_invoices.sql
-- Creates database schemas for sales invoices and detail trip mappings tables

-- 1. Sales Invoices Table
CREATE TABLE sales_invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    invoice_date DATE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'DRAFT' NOT NULL, -- DRAFT, PENDING, APPROVED, GENERATED, CANCELLED
    payment_terms VARCHAR(100),
    subtotal NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    discount NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    net_amount NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_invoice_customer ON sales_invoices(customer_id);

-- 2. Sales Invoice Details Table (Supporting multiple trips per invoice)
CREATE TABLE sales_invoice_details (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES sales_invoices(id) ON DELETE CASCADE,
    trip_id BIGINT REFERENCES trips(id) ON DELETE SET NULL,
    material_id BIGINT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    quantity NUMERIC(12, 2) NOT NULL,
    rate NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    freight_charges NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    loading_charges NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    royalty NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    gst_percentage NUMERIC(5, 2) DEFAULT 18.00 NOT NULL,
    cgst NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    sgst NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    igst NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    net_amount NUMERIC(15, 2) DEFAULT 0.00 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_invoice_detail_invoice ON sales_invoice_details(invoice_id);
CREATE INDEX idx_invoice_detail_trip ON sales_invoice_details(trip_id);
CREATE INDEX idx_invoice_detail_material ON sales_invoice_details(material_id);
