-- ============================================================================
-- V32__auth_management_fields.sql
-- Adds columns to app_users and login_history to support comprehensive 
-- Authentication Management console features.
-- ============================================================================

-- Add authentication and password policy fields to app_users
ALTER TABLE app_users ADD COLUMN last_login TIMESTAMP;
ALTER TABLE app_users ADD COLUMN password_expiry TIMESTAMP DEFAULT CURRENT_TIMESTAMP + INTERVAL '90 days';
ALTER TABLE app_users ADD COLUMN force_password_change BOOLEAN DEFAULT FALSE;
ALTER TABLE app_users ADD COLUMN failed_login_attempts INT DEFAULT 0;

-- Add logout_time to login_history to track sessions and logouts
ALTER TABLE login_history ADD COLUMN logout_time TIMESTAMP;
