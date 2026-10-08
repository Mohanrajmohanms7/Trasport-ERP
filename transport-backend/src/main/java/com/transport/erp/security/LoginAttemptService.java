package com.transport.erp.security;

import com.transport.erp.exception.BusinessValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Wrong-password lockout (5 in a row → 15 minutes) and session versions. Writes run in their own transaction so a
 * failed login (which rolls back the login transaction) still counts.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_FAILURES = 5;
    public static final int LOCK_MINUTES = 15;

    @Autowired private JdbcTemplate jdbc;

    public void assertNotLocked(String username) {
        List<Timestamp> t = jdbc.queryForList("SELECT locked_until FROM app_users WHERE lower(username) = lower(?) AND is_deleted = false",
                Timestamp.class, username);
        if (!t.isEmpty() && t.get(0) != null && t.get(0).toLocalDateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessValidationException("Account Locked", "LOGIN_LOCKED",
                    "Too many wrong passwords. Try again after " + t.get(0).toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ".",
                    "Wait, or ask your administrator to reset your password.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username) {
        jdbc.update("UPDATE app_users SET failed_login_attempts = COALESCE(failed_login_attempts, 0) + 1 WHERE lower(username) = lower(?) AND is_deleted = false", username);
        jdbc.update("UPDATE app_users SET locked_until = ?, failed_login_attempts = 0 WHERE lower(username) = lower(?) AND is_deleted = false AND failed_login_attempts >= ?",
                Timestamp.valueOf(LocalDateTime.now().plusMinutes(LOCK_MINUTES)), username, MAX_FAILURES);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String username) {
        jdbc.update("UPDATE app_users SET failed_login_attempts = 0, locked_until = NULL WHERE lower(username) = lower(?)", username);
    }

    /** Ends every login of this user immediately (their tokens carry the old version; refresh tokens removed). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeSessions(Long userId) {
        jdbc.update("UPDATE app_users SET token_version = COALESCE(token_version, 0) + 1 WHERE id = ?", userId);
        jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
    }

    public int tokenVersion(Long userId) {
        Integer v = jdbc.queryForObject("SELECT COALESCE(token_version, 0) FROM app_users WHERE id = ?", Integer.class, userId);
        return v == null ? 0 : v;
    }
}
