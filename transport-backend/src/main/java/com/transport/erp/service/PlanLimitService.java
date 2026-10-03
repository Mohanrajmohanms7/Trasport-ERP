package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Subscription limits: active trucks, staff logins and branches per client. Limits apply once Platform Admin has
 * applied a plan to the client (companies.plan_enforced); 0 or empty = unlimited. Driver logins are not counted.
 */
@Service
public class PlanLimitService {

    public enum Kind {
        VEHICLES("trucks", "max_vehicles", "SELECT COUNT(*) FROM vehicles WHERE company_id = ? AND is_deleted = false AND status = 'ACTIVE'"),
        USERS("staff logins", "max_users",
                "SELECT COUNT(DISTINCT u.id) FROM app_users u WHERE u.company_id = ? AND u.is_deleted = false AND u.status = 'ACTIVE'"
                        + " AND EXISTS (SELECT 1 FROM user_roles ur JOIN app_roles r ON r.id = ur.role_id WHERE ur.user_id = u.id AND r.code <> 'DRIVER')"),
        BRANCHES("branches", "max_branches", "SELECT COUNT(*) FROM branches WHERE company_id = ? AND is_deleted = false AND status = 'ACTIVE'");

        final String label, column, countSql;
        Kind(String label, String column, String countSql) { this.label = label; this.column = column; this.countSql = countSql; }
    }

    @Autowired private JdbcTemplate jdbc;

    public long used(Long companyId, Kind kind) {
        Long n = jdbc.queryForObject(kind.countSql, Long.class, companyId);
        return n == null ? 0 : n;
    }

    /** Limit for the client, or null when not limited (no plan applied yet, or 0 = unlimited). */
    public Integer limit(Long companyId, Kind kind) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT plan_enforced, " + kind.column + " AS lim FROM companies WHERE id = ?", companyId);
        if (r.isEmpty() || !Boolean.TRUE.equals(r.get(0).get("plan_enforced"))) return null;
        Object lim = r.get(0).get("lim");
        int v = lim == null ? 0 : ((Number) lim).intValue();
        return v <= 0 ? null : v;
    }

    /** Call before creating or re-activating one more truck / staff login / branch. */
    public void assertCanAdd(Long companyId, Kind kind) {
        if (companyId == null) return;
        Integer lim = limit(companyId, kind);
        if (lim == null) return;
        long used = used(companyId, kind);
        if (used >= lim) {
            throw new BusinessValidationException("Plan Limit Reached", "PLAN_LIMIT_" + kind.name(),
                    "Your subscription allows " + lim + " active " + kind.label + " and " + used + " are in use.",
                    "Deactivate one you no longer use, or contact TransaFlow to upgrade your plan.");
        }
    }

    public Map<String, Object> usage(Long companyId) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Kind k : Kind.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("used", used(companyId, k));
            m.put("limit", limit(companyId, k));
            out.put(k.name().toLowerCase(), m);
        }
        return out;
    }
}
