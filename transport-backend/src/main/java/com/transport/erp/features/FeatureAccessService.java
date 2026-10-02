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
        for (FeatureCatalog.Feature x = f; x != null; x = x.parent() == null ? null : FeatureCatalog.get(x.parent())) {
            if (x.core()) continue;
            Boolean v = settings.get(x.code());
            if (!(v != null ? v : FeatureCatalog.defaultOn(x.code()))) return false;
        }
        return true;
    }

    /** Plan settings overlaid with client overrides. */
    private Map<String, Boolean> effectiveSettings(Long companyId) {
        Map<String, Boolean> m = new HashMap<>(planSettings(planOf(companyId)));
        m.putAll(companyOverrides(companyId));
        return m;
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
        jdbc.query("SELECT feature_code, enabled FROM company_features WHERE company_id = ?", rs -> { m.put(rs.getString(1), rs.getBoolean(2)); }, companyId);
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
        Map<String, Boolean> plan = planSettings(planId);
        Map<String, Boolean> overrides = companyOverrides(companyId);
        Set<String> disabled = disabledFor(companyId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", f.code());
            r.put("plan", plan.getOrDefault(f.code(), FeatureCatalog.defaultOn(f.code())));
            r.put("override", overrides.get(f.code()));
            r.put("effective", !disabled.contains(f.code()));
            rows.add(r);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("companyId", companyId);
        out.put("planId", planId);
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
        validateCodes(requested);
        Map<String, Boolean> plan = planSettings(planOf(companyId));
        jdbc.update("DELETE FROM company_features WHERE company_id = ?", companyId);
        for (Map.Entry<String, Boolean> e : requested.entrySet()) {
            FeatureCatalog.Feature f = FeatureCatalog.get(e.getKey());
            if (f.core() || e.getValue() == null) continue;
            boolean planValue = plan.getOrDefault(e.getKey(), FeatureCatalog.defaultOn(e.getKey()));
            if (e.getValue() == planValue) continue;
            jdbc.update("INSERT INTO company_features (company_id, feature_code, enabled, updated_by, updated_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    companyId, e.getKey(), e.getValue(), username);
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
