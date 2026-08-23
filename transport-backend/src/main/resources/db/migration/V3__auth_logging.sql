-- Flyway Migration Script: V3__auth_logging.sql
-- Creates database schema for JWT token management, login history tracker, and transactional audit logs

-- 1. Refresh Tokens Table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE NOT NULL
);
CREATE INDEX idx_refresh_token ON refresh_tokens(token);

-- 2. Login History Table
CREATE TABLE login_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    username VARCHAR(50) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address VARCHAR(50),
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED
    user_agent TEXT
);
CREATE INDEX idx_login_history_user ON login_history(user_id);
CREATE INDEX idx_login_history_time ON login_history(login_time);

-- 3. Audit Logs Table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    entity_id BIGINT,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address VARCHAR(50),
    details TEXT
);
CREATE INDEX idx_audit_log_time ON audit_logs(action_time);
CREATE INDEX idx_audit_log_user ON audit_logs(username);
