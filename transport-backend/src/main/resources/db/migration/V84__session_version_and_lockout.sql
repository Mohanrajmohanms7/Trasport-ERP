-- V84: session control and brute-force protection.
-- token_version: bumped on force logout / admin password reset / deactivation, so existing logins stop working at once.
-- locked_until: set after 5 wrong passwords in a row (15 minutes).
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
UPDATE app_users SET failed_login_attempts = 0 WHERE failed_login_attempts IS NULL;
