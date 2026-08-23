-- ============================================================================
-- V29__saas_platform_admin.sql
-- Creates database tables for Platform Admin (SaaS settings, subscriptions, 
-- licenses, support tickets, announcements, backup history, and billing).
-- ============================================================================

-- 1. SaaS Global Settings
CREATE TABLE saas_system_settings (
    id BIGSERIAL PRIMARY KEY,
    key_name VARCHAR(100) NOT NULL UNIQUE,
    value_data TEXT NOT NULL,
    description TEXT,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. SaaS Subscription Plans
CREATE TABLE saas_plans (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    billing_period VARCHAR(50) NOT NULL DEFAULT 'MONTHLY', -- MONTHLY, YEARLY
    max_users INTEGER NOT NULL DEFAULT 5,
    max_vehicles INTEGER NOT NULL DEFAULT 5,
    max_invoices INTEGER NOT NULL DEFAULT 50,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_date TIMESTAMP
);

-- 3. SaaS Tenant Subscriptions
CREATE TABLE saas_tenant_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED, PENDING
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    amount_paid DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER', -- UPI, BANK_TRANSFER, CARD, CASH
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID', -- PAID, PENDING, FAILED
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_date TIMESTAMP,
    CONSTRAINT fk_tenant_sub_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_tenant_sub_plan FOREIGN KEY (plan_id) REFERENCES saas_plans(id)
);

-- 4. SaaS License Keys
CREATE TABLE saas_licenses (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    license_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, REVOKED, EXPIRED
    activation_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    max_users INTEGER NOT NULL DEFAULT 5,
    max_vehicles INTEGER NOT NULL DEFAULT 5,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_date TIMESTAMP,
    CONSTRAINT fk_saas_lic_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

-- 5. SaaS Support Tickets
CREATE TABLE saas_support_tickets (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    ticket_number VARCHAR(100) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saas_ticket_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

-- 6. SaaS Support Replies
CREATE TABLE saas_support_replies (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    is_admin_reply BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saas_reply_ticket FOREIGN KEY (ticket_id) REFERENCES saas_support_tickets(id) ON DELETE CASCADE
);

-- 7. SaaS Global Announcements
CREATE TABLE saas_announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_date TIMESTAMP
);

-- 8. SaaS Backup Log
CREATE TABLE saas_backups (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_size VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED
    backup_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- SYSTEM, MANUAL
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);

-- 9. SaaS Subscription Billing Invoices
CREATE TABLE saas_billing_invoices (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    invoice_date DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PAID', -- PAID, PENDING, FAILED, CANCELLED
    payment_method VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER',
    transaction_reference VARCHAR(150),
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saas_bill_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

-- ============================================================================
-- Seed platform-level data only (NO company-specific rows).
-- Tenant subscriptions, licenses, tickets, invoices are created by
-- bootstrap / Platform Admin onboarding at runtime — never hardcode company ids.
-- ============================================================================

INSERT INTO saas_system_settings (key_name, value_data, description) VALUES
('SAAS_MAINTENANCE_MODE', 'false', 'Global flag to toggle SaaS maintenance window mode'),
('SAAS_ALLOWED_SIGNUP_DOMAINS', '*', 'Allowed email domains for new tenant registration (* for all)'),
('SAAS_DEFAULT_CURRENCY', 'INR', 'Default system-wide base currency ISO code'),
('SAAS_SMTP_HOST', 'smtp.saas-platform.com', 'SaaS platform outgoing mail server host name');

INSERT INTO saas_plans (code, name, description, price, billing_period, max_users, max_vehicles, max_invoices) VALUES
('TRIAL', 'Trial', 'Trial plan for new tenants', 0.00, 'MONTHLY', 5, 5, 50),
('BASIC', 'Starter Pack', 'Basic tools for owner-operators', 1999.00, 'MONTHLY', 3, 3, 50),
('STANDARD', 'Growth Plan', 'Essential tools for expanding fleets', 4999.00, 'MONTHLY', 10, 15, 500),
('PREMIUM', 'Enterprise Fleet', 'Complete tools with priority support', 9999.00, 'MONTHLY', 50, 100, 5000);
