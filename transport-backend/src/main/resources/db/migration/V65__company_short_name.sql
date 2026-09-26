-- V65: company short name / initials shown above the product name after login (e.g. "PKC").
ALTER TABLE companies ADD COLUMN IF NOT EXISTS short_name VARCHAR(8);
