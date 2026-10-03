-- V79: Help & Support attachments (screenshots, images, PDFs) stored in the database, so they survive redeploys
-- (the container disk on Render is wiped on every deploy) and are included in database backups.
CREATE TABLE saas_support_attachments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES saas_support_tickets(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    data BYTEA NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by VARCHAR(100) NOT NULL,
    from_support BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_support_attachments_ticket ON saas_support_attachments (ticket_id, created_date);
