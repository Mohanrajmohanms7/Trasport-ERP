-- V80: when each user last opened each support ticket — drives the notification bell (unread replies, new tickets).
CREATE TABLE saas_support_ticket_reads (
    ticket_id BIGINT NOT NULL REFERENCES saas_support_tickets(id) ON DELETE CASCADE,
    username VARCHAR(100) NOT NULL,
    last_read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ticket_id, username)
);
CREATE INDEX idx_support_reads_user ON saas_support_ticket_reads (username);
