-- V76: Units of measure for material orders (booking -> trip -> invoice).
--
-- Design (docs/UNITS_OF_MEASURE.md):
--   * uom_master stays the global catalogue (plus optional company-specific rows, unchanged).
--   * company_uoms says which units a company may use on orders, and which one is the default.
--     No row = not enabled. ACTIVE/INACTIVE in `status` switches a unit on/off for that company.
--   * Every booking / trip / invoice line stores the unit it was entered in (uom_id). A trip line takes the
--     unit of its booking line, an invoice line the unit of its trip line. Nothing is converted.

CREATE TABLE company_uoms (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    branch_id BIGINT,
    uom_id BIGINT NOT NULL REFERENCES uom_master(id),
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX uq_company_uoms_company_uom ON company_uoms (company_id, uom_id) WHERE is_deleted = false;
CREATE UNIQUE INDEX uq_company_uoms_one_default ON company_uoms (company_id) WHERE is_deleted = false AND is_default = true;
CREATE INDEX idx_company_uoms_company_status ON company_uoms (company_id, status);

ALTER TABLE booking_details ADD COLUMN uom_id BIGINT REFERENCES uom_master(id);
ALTER TABLE trip_details ADD COLUMN uom_id BIGINT REFERENCES uom_master(id);
ALTER TABLE sales_invoice_details ADD COLUMN uom_id BIGINT REFERENCES uom_master(id);
CREATE INDEX idx_booking_details_uom ON booking_details (uom_id);
CREATE INDEX idx_trip_details_uom ON trip_details (uom_id);
CREATE INDEX idx_sales_invoice_details_uom ON sales_invoice_details (uom_id);

-- Short display symbols so screens read "2 Unit M-Sand" / "5 Ton M-Sand". Only rows still holding the V51 seed value.
UPDATE uom_master SET symbol = 'Unit' WHERE code = 'UNIT' AND company_id IS NULL AND symbol = 'UNIT';
UPDATE uom_master SET symbol = 'Ton'  WHERE code = 'TON'  AND company_id IS NULL AND symbol = 'MT';
UPDATE uom_master SET symbol = 'CFT'  WHERE code = 'CFT'  AND company_id IS NULL AND symbol = 'cu ft';

-- Existing lines were entered on screens labelled "Tons", so they are recorded as TON (no quantity is changed).
UPDATE booking_details SET uom_id = (SELECT id FROM uom_master WHERE code = 'TON' AND company_id IS NULL ORDER BY id LIMIT 1)
 WHERE uom_id IS NULL;
UPDATE trip_details SET uom_id = (SELECT id FROM uom_master WHERE code = 'TON' AND company_id IS NULL ORDER BY id LIMIT 1)
 WHERE uom_id IS NULL;
UPDATE sales_invoice_details SET uom_id = (SELECT id FROM uom_master WHERE code = 'TON' AND company_id IS NULL ORDER BY id LIMIT 1)
 WHERE uom_id IS NULL;

-- Every existing company starts with Unit as its only, default order unit (the admin can switch others on later).
INSERT INTO company_uoms (code, name, status, company_id, uom_id, is_default, created_by, updated_by)
SELECT u.code, u.name, 'ACTIVE', c.id, u.id, true, 'SYSTEM', 'SYSTEM'
  FROM companies c
  JOIN uom_master u ON u.code = 'UNIT' AND u.company_id IS NULL AND u.is_deleted = false
 WHERE c.is_deleted = false
   AND NOT EXISTS (SELECT 1 FROM company_uoms x WHERE x.company_id = c.id AND x.is_deleted = false);
