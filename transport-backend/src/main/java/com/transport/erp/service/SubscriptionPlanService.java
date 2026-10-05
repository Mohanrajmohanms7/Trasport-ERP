package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.features.FeatureAccessService;
import com.transport.erp.features.FeatureCatalog;
import com.transport.erp.model.Company;
import com.transport.erp.model.SaaSPlan;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.SaaSPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Client subscription plans: overview for Platform Admin, change preview (features gained / lost, limits, open work in
 * modules being removed) and applying a plan (features + limits + history). Payment collection is out of scope (later).
 */
@Service
public class SubscriptionPlanService {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private SaaSPlanRepository planRepository;
    @Autowired private FeatureAccessService features;
    @Autowired private PlanLimitService limits;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuditService auditService;

    public List<Map<String, Object>> plans() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SaaSPlan p : planRepository.findAll()) {
            if (Boolean.TRUE.equals(p.getIsDeleted())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("code", p.getCode());
            m.put("name", p.getName());
            m.put("tier", p.getTier());
            m.put("tagline", p.getTagline());
            m.put("description", p.getDescription());
            m.put("price", p.getPrice());
            m.put("billingPeriod", p.getBillingPeriod());
            m.put("maxUsers", p.getMaxUsers());
            m.put("maxVehicles", p.getMaxVehicles());
            m.put("maxBranches", p.getMaxBranches());
            m.put("priceYearly", p.getPriceYearly());
            m.put("extraVehiclePrice", p.getExtraVehiclePrice());
            m.put("extraUserPrice", p.getExtraUserPrice());
            m.put("extraBranchPrice", p.getExtraBranchPrice());
            m.put("setupFee", p.getSetupFee());
            Map<String, Boolean> settings = features.planSettings(p.getId());
            List<String> excluded = new ArrayList<>();
            for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
                if (f.kind() == FeatureCatalog.Kind.MODULE && !f.core() && !features.isOnWith(f.code(), settings)) excluded.add(f.label());
            }
            m.put("excludedModules", excluded);
            out.add(m);
        }
        out.sort(Comparator.comparing(m -> (Integer) m.get("tier")));
        return out;
    }

    public List<Map<String, Object>> overview() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Company c : companyRepository.findAll()) {
            if (Boolean.TRUE.equals(c.getIsDeleted())) continue;
            rows.add(summary(c));
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("name"))));
        return rows;
    }

    private Map<String, Object> summary(Company c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("companyId", c.getId());
        m.put("name", c.getName());
        m.put("shortName", c.getDisplayShortName());
        SaaSPlan p = c.getSubscriptionPlan();
        m.put("planId", p == null ? null : p.getId());
        m.put("planName", p == null ? null : p.getName());
        m.put("planTier", p == null ? null : p.getTier());
        m.put("planEnforced", Boolean.TRUE.equals(c.getPlanEnforced()));
        m.put("status", c.getSubscriptionStatus());
        m.put("startDate", c.getSubscriptionStartDate());
        m.put("endDate", c.getSubscriptionEndDate());
        m.put("usage", limits.usage(c.getId()));
        Set<String> off = features.disabledFor(c.getId());
        long modules = FeatureCatalog.all().stream().filter(f -> f.kind() == FeatureCatalog.Kind.MODULE && !f.core()).count();
        long offModules = FeatureCatalog.all().stream().filter(f -> f.kind() == FeatureCatalog.Kind.MODULE && !f.core() && off.contains(f.code())).count();
        m.put("modulesEnabled", modules - offModules);
        m.put("modulesTotal", modules);
        m.put("overrides", features.companyOverrides(c.getId()).size());
        m.put("extras", Map.of("vehicles", nz(c.getExtraVehicles()), "users", nz(c.getExtraUsers()), "branches", nz(c.getExtraBranches())));
        m.put("billingCycle", c.getBillingCycle());
        m.put("billing", billing(c));
        return m;
    }

    public Map<String, Object> detail(Long companyId) {
        Company c = company(companyId);
        Map<String, Object> m = summary(c);
        Set<String> off = features.disabledFor(companyId);
        List<String> enabled = new ArrayList<>(), disabled = new ArrayList<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
            if (f.kind() != FeatureCatalog.Kind.MODULE || f.core()) continue;
            (off.contains(f.code()) ? disabled : enabled).add(f.label());
        }
        m.put("enabledModules", enabled);
        m.put("disabledModules", disabled);
        m.put("features", features.companyView(companyId).get("features"));
        m.put("history", jdbc.queryForList(
                "SELECT h.changed_at, h.change_type, h.note, h.changed_by, fp.name AS from_plan, tp.name AS to_plan FROM plan_change_history h"
                        + " LEFT JOIN saas_plans fp ON fp.id = h.from_plan_id JOIN saas_plans tp ON tp.id = h.to_plan_id"
                        + " WHERE h.company_id = ? ORDER BY h.changed_at DESC LIMIT 50", companyId));
        return m;
    }

    /** What happens if the client moves to this plan (nothing is changed). */
    public Map<String, Object> preview(Long companyId, Long planId) {
        Company c = company(companyId);
        SaaSPlan target = plan(planId);
        Set<String> now = features.disabledFor(companyId);
        Set<String> after = features.disabledWithPlan(companyId, planId);
        List<String> gained = new ArrayList<>(), lost = new ArrayList<>();
        for (FeatureCatalog.Feature f : FeatureCatalog.all()) {
            if (f.core()) continue;
            String name = f.parent() == null ? f.label() : labelOf(f.parent()) + " → " + f.label();
            if (now.contains(f.code()) && !after.contains(f.code())) gained.add(name);
            if (!now.contains(f.code()) && after.contains(f.code())) lost.add(name);
        }
        List<String> warnings = new ArrayList<>();
        Map<String, Object> usage = limits.usage(companyId);
        checkLimit(warnings, "trucks", usage, "vehicles", target.getMaxVehicles());
        checkLimit(warnings, "staff logins", usage, "users", target.getMaxUsers());
        checkLimit(warnings, "branches", usage, "branches", target.getMaxBranches());
        if (after.contains("work-orders") && !now.contains("work-orders")) {
            long open = count("SELECT COUNT(*) FROM work_orders WHERE company_id = ? AND is_deleted = false AND status IN ('OPEN','IN_PROGRESS')", companyId);
            if (open > 0) warnings.add(open + " open work order(s): those trucks stay blocked from trips until the jobs are closed — close them before downgrading.");
        }
        if (after.contains("payables") && !now.contains("payables")) {
            long unpaid = count("SELECT COUNT(*) FROM supplier_bills WHERE company_id = ? AND is_deleted = false AND status = 'APPROVED' AND payment_status <> 'PAID'", companyId);
            if (unpaid > 0) warnings.add(unpaid + " unpaid supplier bill(s) will no longer be visible.");
        }
        if (after.contains("payroll") && !now.contains("payroll")) {
            long open = count("SELECT COUNT(*) FROM driver_payrolls WHERE company_id = ? AND is_deleted = false AND status IN ('DRAFT','APPROVED','POSTED')", companyId);
            if (open > 0) warnings.add(open + " driver payroll(s) not yet paid will no longer be visible.");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("currentPlan", c.getSubscriptionPlan() == null ? null : c.getSubscriptionPlan().getName());
        m.put("targetPlan", target.getName());
        m.put("changeType", changeType(c, target));
        m.put("gained", gained);
        m.put("lost", lost);
        m.put("warnings", warnings);
        m.put("limits", Map.of("users", target.getMaxUsers(), "vehicles", target.getMaxVehicles(), "branches", target.getMaxBranches()));
        return m;
    }

    @Transactional
    public Map<String, Object> apply(Long companyId, Long planId, String note, String username) {
        Company c = company(companyId);
        SaaSPlan target = plan(planId);
        String type = changeType(c, target);
        Long fromPlan = c.getSubscriptionPlan() == null ? null : c.getSubscriptionPlan().getId();
        c.setSubscriptionPlan(target);
        c.setPlanEnforced(true);
        applyLimits(c, target);
        c.setUpdatedBy(username);
        companyRepository.saveAndFlush(c);   // flush so the usage/limit read below (JDBC) sees it
        jdbc.update("INSERT INTO plan_change_history (company_id, from_plan_id, to_plan_id, change_type, note, changed_by) VALUES (?, ?, ?, ?, ?, ?)",
                companyId, fromPlan, target.getId(), type, note, username);
        features.evict(companyId);
        auditService.log(username, "PLAN_" + type, "companies", companyId, null,
                c.getName() + " → " + target.getName() + (note == null || note.isBlank() ? "" : " (" + note + ")"));
        return detail(companyId);
    }

    private String changeType(Company c, SaaSPlan target) {
        if (!Boolean.TRUE.equals(c.getPlanEnforced()) || c.getSubscriptionPlan() == null) return "APPLIED";
        int from = Optional.ofNullable(c.getSubscriptionPlan().getTier()).orElse(0);
        int to = Optional.ofNullable(target.getTier()).orElse(0);
        return to > from ? "UPGRADE" : to < from ? "DOWNGRADE" : "SAME";
    }

    private void checkLimit(List<String> warnings, String label, Map<String, Object> usage, String key, Integer limit) {
        if (limit == null || limit <= 0) return;
        @SuppressWarnings("unchecked") Map<String, Object> u = (Map<String, Object>) usage.get(key);
        long used = ((Number) u.get("used")).longValue();
        if (used > limit) warnings.add("Uses " + used + " active " + label + " but the plan allows " + limit
                + ": nothing is removed, but no more can be added until they are under the limit.");
    }

    private String labelOf(String code) {
        FeatureCatalog.Feature f = FeatureCatalog.get(code);
        return f == null ? code : f.label();
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private Company company(Long id) {
        return companyRepository.findById(id).filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
    }

    private SaaSPlan plan(Long id) {
        SaaSPlan p = planRepository.findById(id).filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException("Plan Not Found", "PLAN_NOT_FOUND", "Plan " + id + " does not exist.", "Pick a plan from the list."));
        if (!"ACTIVE".equalsIgnoreCase(String.valueOf(p.getStatus()))) {
            throw new BusinessValidationException("Plan Inactive", "PLAN_INACTIVE", p.getName() + " is not active.", "Activate the plan first.");
        }
        return p;
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    /** Client limit = plan limit + purchased extras (a plan limit of 0 means unlimited and stays unlimited). */
    private void applyLimits(Company c, SaaSPlan p) {
        c.setMaxVehicles(plus(p.getMaxVehicles(), c.getExtraVehicles()));
        c.setMaxUsers(plus(p.getMaxUsers(), c.getExtraUsers()));
        c.setMaxBranches(plus(p.getMaxBranches(), c.getExtraBranches()));
    }

    private static Integer plus(Integer planLimit, Integer extra) {
        if (planLimit == null || planLimit <= 0) return 0;
        return planLimit + nz(extra);
    }

    /** After a plan's limits change: every client on it (with a plan applied) gets the new limits. */
    @Transactional
    public int syncLimitsForPlan(Long planId, String username) {
        int n = 0;
        for (Company c : companyRepository.findAll()) {
            if (Boolean.TRUE.equals(c.getIsDeleted()) || !Boolean.TRUE.equals(c.getPlanEnforced())
                    || c.getSubscriptionPlan() == null || !planId.equals(c.getSubscriptionPlan().getId())) continue;
            applyLimits(c, c.getSubscriptionPlan());
            c.setUpdatedBy(username);
            companyRepository.saveAndFlush(c);   // flush so the usage/limit read below (JDBC) sees it
            n++;
        }
        return n;
    }

    /** Extra trucks / users / branches bought by the client and its billing cycle. */
    @Transactional
    public Map<String, Object> setExtras(Long companyId, Integer vehicles, Integer users, Integer branches, String cycle, String username) {
        Company c = company(companyId);
        for (Integer v : new Integer[]{vehicles, users, branches}) {
            if (v != null && (v < 0 || v > 100000)) {
                throw new BusinessValidationException("Invalid Extras", "PLAN_EXTRAS_INVALID", "Extras must be 0 or more.", "Correct the numbers.");
            }
        }
        if (vehicles != null) c.setExtraVehicles(vehicles);
        if (users != null) c.setExtraUsers(users);
        if (branches != null) c.setExtraBranches(branches);
        if (cycle != null && !cycle.isBlank()) {
            String cy = cycle.trim().toUpperCase(Locale.ROOT);
            if (!cy.equals("MONTHLY") && !cy.equals("YEARLY")) {
                throw new BusinessValidationException("Invalid Billing Cycle", "PLAN_CYCLE_INVALID", "Billing cycle must be MONTHLY or YEARLY.", "Pick one.");
            }
            c.setBillingCycle(cy);
        }
        if (Boolean.TRUE.equals(c.getPlanEnforced()) && c.getSubscriptionPlan() != null) applyLimits(c, c.getSubscriptionPlan());
        c.setUpdatedBy(username);
        companyRepository.saveAndFlush(c);   // flush so the usage/limit read below (JDBC) sees it
        auditService.log(username, "PLAN_EXTRAS_UPDATED", "companies", companyId, null,
                "Extras: trucks " + nz(c.getExtraVehicles()) + ", users " + nz(c.getExtraUsers()) + ", branches " + nz(c.getExtraBranches())
                        + ", billing " + c.getBillingCycle());
        return detail(companyId);
    }

    /** What the client pays (estimate; collection is manual for now). */
    public Map<String, Object> billing(Company c) {
        Map<String, Object> b = new LinkedHashMap<>();
        SaaSPlan p = c.getSubscriptionPlan();
        if (p == null) return b;
        java.math.BigDecimal monthlyPlan = nzd(p.getPrice());
        java.math.BigDecimal yearlyPlan = p.getPriceYearly() != null ? p.getPriceYearly() : monthlyPlan.multiply(java.math.BigDecimal.valueOf(12));
        java.math.BigDecimal extrasMonthly = nzd(p.getExtraVehiclePrice()).multiply(java.math.BigDecimal.valueOf(nz(c.getExtraVehicles())))
                .add(nzd(p.getExtraUserPrice()).multiply(java.math.BigDecimal.valueOf(nz(c.getExtraUsers()))))
                .add(nzd(p.getExtraBranchPrice()).multiply(java.math.BigDecimal.valueOf(nz(c.getExtraBranches()))));
        boolean yearly = "YEARLY".equals(c.getBillingCycle());
        java.math.BigDecimal perCycle = yearly ? yearlyPlan.add(extrasMonthly.multiply(java.math.BigDecimal.valueOf(12))) : monthlyPlan.add(extrasMonthly);
        b.put("cycle", yearly ? "YEARLY" : "MONTHLY");
        b.put("planPrice", yearly ? yearlyPlan : monthlyPlan);
        b.put("extrasPerMonth", extrasMonthly);
        b.put("amountPerCycle", perCycle);
        b.put("perMonthEquivalent", yearly ? perCycle.divide(java.math.BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP) : perCycle);
        b.put("setupFee", nzd(p.getSetupFee()));
        return b;
    }

    private static java.math.BigDecimal nzd(java.math.BigDecimal v) { return v == null ? java.math.BigDecimal.ZERO : v; }
}
