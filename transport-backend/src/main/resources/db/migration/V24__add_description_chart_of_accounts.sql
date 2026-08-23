-- Flyway Migration Script: V24__add_description_chart_of_accounts.sql
-- Adds missing description column to chart_of_accounts table

ALTER TABLE chart_of_accounts ADD COLUMN IF NOT EXISTS description TEXT;
