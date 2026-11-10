-- V92: role permissions per company: module x View / Add / Edit / Delete / Approve.
-- No rows for a company's role = full access (exactly the behaviour before V92); rows are written only when an admin
-- saves that role's grid. SUPER_ADMIN and the DRIVER role are not managed here.
CREATE TABLE IF NOT EXISTS role_module_permissions (
    company_id  BIGINT      NOT NULL REFERENCES companies(id),
    role_code   VARCHAR(50) NOT NULL,
    module      VARCHAR(40) NOT NULL,
    can_view    BOOLEAN     NOT NULL DEFAULT TRUE,
    can_add     BOOLEAN     NOT NULL DEFAULT TRUE,
    can_edit    BOOLEAN     NOT NULL DEFAULT TRUE,
    can_delete  BOOLEAN     NOT NULL DEFAULT TRUE,
    can_approve BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by  VARCHAR(100),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, role_code, module)
);
