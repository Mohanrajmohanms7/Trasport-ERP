-- V72: vehicles and drivers are based at a branch. Those saved without one belong to the head office
-- (lowest active branch of the company). Customers and suppliers stay company-wide (branch optional).
UPDATE vehicles x SET branch_id = (SELECT MIN(b.id) FROM branches b WHERE b.company_id = x.company_id AND b.is_deleted = FALSE)
 WHERE x.branch_id IS NULL AND x.company_id IS NOT NULL;
UPDATE drivers x SET branch_id = (SELECT MIN(b.id) FROM branches b WHERE b.company_id = x.company_id AND b.is_deleted = FALSE)
 WHERE x.branch_id IS NULL AND x.company_id IS NOT NULL;
