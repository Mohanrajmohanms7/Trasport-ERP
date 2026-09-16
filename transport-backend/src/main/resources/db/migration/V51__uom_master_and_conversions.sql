-- Flyway Migration Script: V51__uom_master_and_conversions.sql
-- UOM Master and Material-Based UOM Conversion Engine

-- 1. Create uom_master table
CREATE TABLE uom_master (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    symbol VARCHAR(20),
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    is_base_unit BOOLEAN DEFAULT FALSE NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

-- 2. Create uom_conversions table
CREATE TABLE uom_conversions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    material_id BIGINT REFERENCES materials(id) ON DELETE CASCADE,
    from_uom_id BIGINT NOT NULL REFERENCES uom_master(id) ON DELETE CASCADE,
    to_uom_id BIGINT NOT NULL REFERENCES uom_master(id) ON DELETE CASCADE,
    conversion_factor NUMERIC(14, 6) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    company_id BIGINT,
    branch_id BIGINT,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR(50) DEFAULT 'SYSTEM',
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    version INT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_uom_conv_material ON uom_conversions(material_id);
CREATE INDEX idx_uom_conv_from_to ON uom_conversions(from_uom_id, to_uom_id);

-- 3. Extend materials table with default_uom_id
ALTER TABLE materials ADD COLUMN default_uom_id BIGINT REFERENCES uom_master(id);
CREATE INDEX idx_materials_default_uom ON materials(default_uom_id);

-- 4. Seed Standard UOMs
INSERT INTO uom_master (code, name, symbol, category, is_base_unit, description, status) VALUES
('TON', 'Metric Tonne', 'MT', 'WEIGHT', TRUE, 'Metric Ton (1,000 kg)', 'ACTIVE'),
('KG', 'Kilogram', 'kg', 'WEIGHT', FALSE, 'Kilogram standard weight unit', 'ACTIVE'),
('CFT', 'Cubic Feet', 'cu ft', 'VOLUME', FALSE, 'Cubic feet volume unit for bulk materials', 'ACTIVE'),
('UNIT', 'Unit (100 CFT)', 'UNIT', 'VOLUME', TRUE, '1 Unit = 100 CFT (~4.53 MT / 4530 kg)', 'ACTIVE'),
('BAG', 'Bag', 'bag', 'PACKAGING', TRUE, 'Standard packaging bag (e.g. Cement 50kg)', 'ACTIVE'),
('NOS', 'Numbers / Pieces', 'nos', 'COUNT', TRUE, 'Individual piece or item count', 'ACTIVE'),
('LITRE', 'Litre', 'L', 'LIQUID_VOLUME', TRUE, 'Liquid volume measure', 'ACTIVE'),
('M3', 'Cubic Meter', 'm³', 'VOLUME', FALSE, 'Cubic meter standard metric volume', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- 5. Seed Standard UOM Conversions
-- Note: from_uom and to_uom IDs resolved dynamically via subqueries
INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_UNIT_CFT', '1 UNIT to 100 CFT', NULL, u1.id, u2.id, 100.000000, 'Global Conversion: 1 UNIT = 100 CFT', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'UNIT' AND u2.code = 'CFT'
ON CONFLICT DO NOTHING;

INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_CFT_UNIT', '1 CFT to 0.01 UNIT', NULL, u1.id, u2.id, 0.010000, 'Global Conversion: 1 CFT = 0.01 UNIT', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'CFT' AND u2.code = 'UNIT'
ON CONFLICT DO NOTHING;

INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_TON_KG', '1 TON to 1000 KG', NULL, u1.id, u2.id, 1000.000000, 'Global Conversion: 1 TON = 1000 KG', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'TON' AND u2.code = 'KG'
ON CONFLICT DO NOTHING;

INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_KG_TON', '1 KG to 0.001 TON', NULL, u1.id, u2.id, 0.001000, 'Global Conversion: 1 KG = 0.001 TON', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'KG' AND u2.code = 'TON'
ON CONFLICT DO NOTHING;

-- Business Rule Conversions (1 UNIT ≈ 4.53 Metric Tonnes / 4,530 kg)
INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_UNIT_TON', '1 UNIT to 4.53 TON', NULL, u1.id, u2.id, 4.530000, 'Global Standard: 1 UNIT = 4.53 Metric Tonnes', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'UNIT' AND u2.code = 'TON'
ON CONFLICT DO NOTHING;

INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_TON_UNIT', '1 TON to 0.220751 UNIT', NULL, u1.id, u2.id, 0.220751, 'Global Standard: 1 Metric Tonne = 0.220751 UNIT', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'TON' AND u2.code = 'UNIT'
ON CONFLICT DO NOTHING;

INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_UNIT_KG', '1 UNIT to 4530 KG', NULL, u1.id, u2.id, 4530.000000, 'Global Standard: 1 UNIT = 4,530 kg', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'UNIT' AND u2.code = 'KG'
ON CONFLICT DO NOTHING;

INSERT INTO uom_conversions (code, name, material_id, from_uom_id, to_uom_id, conversion_factor, description, status)
SELECT 
    'CONV_KG_UNIT', '1 KG to 0.00022075 UNIT', NULL, u1.id, u2.id, 0.00022075, 'Global Standard: 1 kg = 0.00022075 UNIT', 'ACTIVE'
FROM uom_master u1, uom_master u2
WHERE u1.code = 'KG' AND u2.code = 'UNIT'
ON CONFLICT DO NOTHING;
