package com.transport.erp.features;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which features a client (company) may use. Effective value per feature = client override, else plan setting,
 * else allowed. A feature is usable only if it and all its parents are enabled. Core features are always on.
 * Results are cached per company for 60 s and refreshed immediately when Platform Admin saves changes.
 */
@Service
public class FeatureAccessService {

    private static final long TTL_MS = 60_000;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private TenantAccessService tenantAccess;

    private record Cached(Set<String> disabled, long at) { }
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    /** Codes switched off for a company (empty = everything allowed). */
    public Set<String> disabledFor(Long companyId) {
        if (companyId == null) return Set.of();
        Cached c = cache.get(companyId);
        if (c != null && System.currentTimeMillis() - c.at() < TTL_MS) return c.disabled();
        Map<String, Boolean> effective = effectiveSettings(companyId);
        Set<String> disabled = new HashSet<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
            if (!isOn(f, effective)) disabled.add(f.code());
        }
        Set<String> result = Collections.unmodifiableSet(disabled);
        cache.put(companyId, new Cached(result, System.currentTimeMillis()));
        return result;
    }

    private boolean isOn(FeatureCatalog.Feature f, Map<String, Boolean> settings) {
        return isOn(f, settings, new HashSet<>());
    }

    /** On only if it, its parents and everything it requires are on. */
    private boolean isOn(FeatureCatalog.Feature f, Map<String, Boolean> settings, Set<String> seen) {
        if (f == null || !seen.add(f.code())) return true;
        for (FeatureCatalog.Feature x = f; x != null; x = x.parent() == null ? null : FeatureCatalog.get(x.parent())) {
            if (!x.core()) {
                Boolean v = settings.get(x.code());
                if (!(v != null ? v : FeatureCatalog.defaultOn(x.code()))) return false;
            }
            for (String r : FeatureCatalog.requires(x.code())) {
                if (!isOn(FeatureCatalog.get(r), settings, seen)) return false;
            }
        }
        return true;
    }

    /** Plan settings overlaid with client overrides. */
    private Map<String, Boolean> effectiveSettings(Long companyId) {
        // Legacy clients (no plan applied yet) keep full access; their own overrides still apply.
        Map<String, Boolean> m = new HashMap<>(planEnforced(companyId) ? planSettings(planOf(companyId)) : Map.of());
        m.putAll(companyOverrides(companyId));
        return m;
    }

    /** Whether a feature would be on with these settings (dependencies and parents included). */
    public boolean isOnWith(String code, Map<String, Boolean> settings) {
        return isOn(FeatureCatalog.get(code), settings);
    }

    /** Features that would be off if the client moved to this plan (client overrides kept). */
    public Set<String> disabledWithPlan(Long companyId, Long planId) {
        Map<String, Boolean> m = new HashMap<>(planSettings(planId));
        m.putAll(companyOverrides(companyId));
        Set<String> off = new HashSet<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) if (!isOn(f, m)) off.add(f.code());
        return off;
    }

    public boolean planEnforced(Long companyId) {
        List<Boolean> v = jdbc.queryForList("SELECT plan_enforced FROM companies WHERE id = ?", Boolean.class, companyId);
        return !v.isEmpty() && Boolean.TRUE.equals(v.get(0));
    }

    private Long planOf(Long companyId) {
        return companyRepository.findById(companyId).map(c -> c.getSubscriptionPlan() == null ? null : c.getSubscriptionPlan().getId()).orElse(null);
    }

    public Map<String, Boolean> planSettings(Long planId) {
        Map<String, Boolean> m = new HashMap<>();
        if (planId == null) return m;
        jdbc.query("SELECT feature_code, enabled FROM plan_features WHERE plan_id = ?", rs -> { m.put(rs.getString(1), rs.getBoolean(2)); }, planId);
        return m;
    }

    public Map<String, Boolean> companyOverrides(Long companyId) {
        Map<String, Boolean> m = new HashMap<>();
        // Overrides past their "valid until" date no longer count.
        jdbc.query("SELECT feature_code, enabled FROM company_features WHERE company_id = ? AND (valid_until IS NULL OR valid_until >= CURRENT_DATE)",
                rs -> { m.put(rs.getString(1), rs.getBoolean(2)); }, companyId);
        return m;
    }

    public boolean isEnabled(Long companyId, String code) {
        return code == null || !disabledFor(companyId).contains(code);
    }

    /** For services that decide finer than URLs (e.g. report categories). Platform admins are never limited. */
    public void require(String code) {
        AppUser u = tenantAccess.requireCurrentUser();
        if (tenantAccess.isSuperAdmin(u)) return;
        if (!isEnabled(u.getCompanyId(), code)) {
            FeatureCatalog.Feature f = FeatureCatalog.get(code);
            throw new BusinessValidationException("Not In Your Plan", "FEATURE_DISABLED",
                    (f != null ? f.label() : code) + " is not included in your subscription.", "Contact TransaFlow to enable it.");
        }
    }

    // ---------------------------------------------------------------- Platform Admin

    public List<Map<String, Object>> catalog() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", f.code());
            m.put("parent", f.parent());
            m.put("group", f.group());
            m.put("label", f.label());
            m.put("description", f.description());
            m.put("kind", f.kind().name());
            m.put("core", f.core());
            m.put("defaultOn", FeatureCatalog.defaultOn(f.code()));
            m.put("routes", f.routes());
            m.put("tabKey", f.tabKey());
            out.add(m);
        }
        return out;
    }

    /** Per feature: plan value, client override, effective value — for the Platform Admin screen. */
    public Map<String, Object> companyView(Long companyId) {
        companyRepository.findById(companyId).orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        Long planId = planOf(companyId);
        boolean enforced = planEnforced(companyId);
        Map<String, Boolean> plan = enforced ? planSettings(planId) : Map.of();
        Map<String, Boolean> overrides = companyOverrides(companyId);
        Map<String, Map<String, Object>> details = new HashMap<>();
        jdbc.query("SELECT feature_code, reason, valid_until, updated_by, updated_date FROM company_features WHERE company_id = ?", rs -> {
            Map<String, Object> d = new HashMap<>();
            d.put("reason", rs.getString(2));
            d.put("validUntil", rs.getDate(3) == null ? null : rs.getDate(3).toLocalDate().toString());
            d.put("by", rs.getString(4));
            d.put("at", rs.getTimestamp(5) == null ? null : rs.getTimestamp(5).toLocalDateTime().toString());
            details.put(rs.getString(1), d);
        }, companyId);
        Set<String> disabled = disabledFor(companyId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", f.code());
            r.put("plan", plan.getOrDefault(f.code(), FeatureCatalog.defaultOn(f.code())));
            r.put("override", overrides.get(f.code()));
            if (details.containsKey(f.code())) r.put("overrideInfo", details.get(f.code()));
            r.put("effective", !disabled.contains(f.code()));
            rows.add(r);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("companyId", companyId);
        out.put("planId", planId);
        out.put("planEnforced", enforced);
        out.put("features", rows);
        return out;
    }

    public Map<String, Object> planView(Long planId) {
        Map<String, Boolean> plan = planSettings(planId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
            rows.add(Map.of("code", f.code(), "enabled", plan.getOrDefault(f.code(), FeatureCatalog.defaultOn(f.code()))));
        }
        return Map.of("planId", planId, "features", rows);
    }

    /**
     * Save a client's access. Values equal to the plan are stored as "follow plan" (no override), so later plan changes
     * still flow to the client unless deliberately overridden.
     */
    @Transactional
    public Map<String, Object> saveCompany(Long companyId, Map<String, Boolean> requested, String username) {
        return saveCompany(companyId, requested, null, null, username);
    }

    /** reason / validUntil are recorded on overrides created or changed by this save (add-on, module trial, goodwill…). */
    @Transactional
    public Map<String, Object> saveCompany(Long companyId, Map<String, Boolean> requested, String reason, java.time.LocalDate validUntil, String username) {
        validateCodes(requested);
        Map<String, Object[]> previous = new HashMap<>();
        jdbc.query("SELECT feature_code, enabled, reason, valid_until FROM company_features WHERE company_id = ?",
                rs -> { previous.put(rs.getString(1), new Object[]{rs.getBoolean(2), rs.getString(3), rs.getDate(4)}); }, companyId);
        Map<String, Boolean> plan = planEnforced(companyId) ? planSettings(planOf(companyId)) : Map.of();
        jdbc.update("DELETE FROM company_features WHERE company_id = ?", companyId);
        for (Map.Entry<String, Boolean> e : requested.entrySet()) {
            FeatureCatalog.Feature f = FeatureCatalog.get(e.getKey());
            if (f.core() || e.getValue() == null) continue;
            boolean planValue = plan.getOrDefault(e.getKey(), FeatureCatalog.defaultOn(e.getKey()));
            if (e.getValue() == planValue) continue;
            Object[] prev = previous.get(e.getKey());
            boolean unchanged = prev != null && e.getValue().equals(prev[0]);
            String r = unchanged && (reason == null || reason.isBlank()) ? (String) prev[1] : reason;
            Object until = unchanged && validUntil == null ? prev[2] : (validUntil == null ? null : java.sql.Date.valueOf(validUntil));
            jdbc.update("INSERT INTO company_features (company_id, feature_code, enabled, reason, valid_until, updated_by, updated_date) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    companyId, e.getKey(), e.getValue(), r, until, username);
        }
        cache.remove(companyId);
        return companyView(companyId);
    }

    @Transactional
    public Map<String, Object> resetCompany(Long companyId) {
        jdbc.update("DELETE FROM company_features WHERE company_id = ?", companyId);
        cache.remove(companyId);
        return companyView(companyId);
    }

    @Transactional
    public Map<String, Object> savePlan(Long planId, Map<String, Boolean> requested, String username) {
        validateCodes(requested);
        jdbc.update("DELETE FROM plan_features WHERE plan_id = ?", planId);
        for (Map.Entry<String, Boolean> e : requested.entrySet()) {
            FeatureCatalog.Feature f = FeatureCatalog.get(e.getKey());
            // Only store what differs from the default (OFF for normal features, ON for opt-in add-ons).
            if (f.core() || e.getValue() == null || e.getValue() == FeatureCatalog.defaultOn(e.getKey())) continue;
            jdbc.update("INSERT INTO plan_features (plan_id, feature_code, enabled, updated_by, updated_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    planId, e.getKey(), e.getValue(), username);
        }
        cache.clear();
        return planView(planId);
    }

    private static void validateCodes(Map<String, Boolean> requested) {
        if (requested == null) throw new IllegalArgumentException("No feature settings sent.");
        List<String> unknown = requested.keySet().stream().filter(k -> !FeatureCatalog.exists(k)).toList();
        if (!unknown.isEmpty()) {
            throw new BusinessValidationException("Unknown Feature", "FEATURE_UNKNOWN", "Unknown feature code(s): " + unknown, "Reload the page.");
        }
    }

    public void evict(Long companyId) {
        if (companyId == null) cache.clear(); else cache.remove(companyId);
    }
}
