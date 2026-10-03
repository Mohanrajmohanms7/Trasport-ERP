-- V78: client issue reporting (Help & Support) on top of the V29 support tables. See docs/SUPPORT_TICKETS.md.

ALTER TABLE saas_support_tickets
    ADD COLUMN module VARCHAR(80),
    ADD COLUMN screen VARCHAR(150),
    ADD COLUMN page_url VARCHAR(500),
    ADD COLUMN record_type VARCHAR(50),
    ADD COLUMN record_id BIGINT,
    ADD COLUMN record_label VARCHAR(150),
    ADD COLUMN user_agent VARCHAR(500),
    ADD COLUMN app_version VARCHAR(50),
    ADD COLUMN assigned_to VARCHAR(100),
    ADD COLUMN last_client_activity TIMESTAMP,
    ADD COLUMN last_admin_activity TIMESTAMP,
    ADD COLUMN first_responded_at TIMESTAMP,
    ADD COLUMN first_response_due TIMESTAMP,
    ADD COLUMN resolve_due TIMESTAMP,
    ADD COLUMN resolution TEXT,
    ADD COLUMN resolved_at TIMESTAMP,
    ADD COLUMN resolved_by VARCHAR(100),
    ADD COLUMN closed_at TIMESTAMP,
    ADD COLUMN reopen_count INT NOT NULL DEFAULT 0,
    ADD COLUMN version INT NOT NULL DEFAULT 0;

-- Priorities are LOW / MEDIUM / HIGH / CRITICAL (V29 used URGENT for the top one).
UPDATE saas_support_tickets SET priority = 'CRITICAL' WHERE priority = 'URGENT';
UPDATE saas_support_tickets SET last_client_activity = created_date WHERE last_client_activity IS NULL;

-- Internal notes are never shown to the client (filtered on the server).
ALTER TABLE saas_support_replies ADD COLUMN is_internal BOOLEAN NOT NULL DEFAULT FALSE;

-- Ticket history: created, status / priority / assignee changes, resolved, reopened, closed.
CREATE TABLE saas_support_ticket_events (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES saas_support_tickets(id) ON DELETE CASCADE,
    username VARCHAR(100) NOT NULL,
    action VARCHAR(40) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_support_events_ticket ON saas_support_ticket_events (ticket_id, created_date);

-- Ticket numbers per client: <client prefix>-TKT-00001 (e.g. PKC-TKT-00001). The prefix is fixed at the first ticket.
CREATE TABLE saas_support_ticket_counters (
    company_id BIGINT PRIMARY KEY REFERENCES companies(id),
    prefix VARCHAR(40) NOT NULL UNIQUE,
    last_number BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_support_tickets_company_status ON saas_support_tickets (company_id, status);
CREATE INDEX idx_support_tickets_status_priority ON saas_support_tickets (status, priority);
CREATE INDEX idx_support_tickets_assigned ON saas_support_tickets (assigned_to);
CREATE INDEX idx_support_tickets_created ON saas_support_tickets (created_date);
CREATE INDEX idx_support_replies_ticket ON saas_support_replies (ticket_id, created_date);
