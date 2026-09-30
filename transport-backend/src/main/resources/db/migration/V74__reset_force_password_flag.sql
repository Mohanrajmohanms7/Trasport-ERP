-- V74: "force password change" was set on every onboarded admin but never enforced. It is now enforced, so clear it for
-- existing logins (they already use their passwords); from now on it is set only for system-generated or reset passwords.
UPDATE app_users SET force_password_change = FALSE WHERE force_password_change = TRUE;
