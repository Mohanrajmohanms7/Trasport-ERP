package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Role permissions per company: module x View / Add / Edit / Delete / Approve (V92). A role with no saved grid has full
 * access (the behaviour before V92). Checked centrally for every API call by ModulePermissionFilter.
 */
@Service
public class RolePermissionService {

    public enum Action { VIEW, ADD, EDIT, DELETE, APPROVE }

    /** Module key, label, API path prefixes (after /api/v1), and whether its lists stay readable for other screens' dropdowns. */
    public record Module(String key, String label, List<String> prefixes, boolean listsOpen) {}

    public static final List<Module> MODULES = List.of(
            new Module("dashboard", "Dashboard", List.of("/dashboard"), false),
            new Module("masters", "Customers & Masters", List.of("/customers", "/materials", "/suppliers", "/quarries", "/loading-locations", "/customer-delivery-sites", "/delivery-sites"), true),
            new Module("vehicles", "Vehicles", List.of("/vehicles"), true),
            new Module("drivers", "Drivers", List.of("/drivers", "/driver-salaries", "/driver-attendance"), true),
            new Module("bookings", "Bookings", List.of("/bookings"), true),
            new Module("trips", "Trips & Dispatch", List.of("/trips"), true),
            new Module("fuel", "Fuel", List.of("/fuel", "/fuel-requests"), false),
            new Module("expenses", "Expenses", List.of("/expenses"), false),
            new Module("maintenance", "Maintenance & Stores", List.of("/maintenance-requests", "/work-orders", "/spare-parts", "/inventory", "/stock", "/warehouses", "/stores"), false),
            new Module("invoices", "Invoices", List.of("/invoices", "/exports/invoices"), false),
            new Module("receipts", "Receipts", List.of("/receipts", "/customer-receipts", "/payments"), false),
            new Module("payables", "Supplier Bills & Payments", List.of("/supplier-bills", "/supplier-payments", "/payables"), false),
            new Module("accounts", "Accounts", List.of("/journal", "/accounts", "/financial-reports", "/ledger"), false),
            new Module("payroll", "Driver Payroll", List.of("/driver-payrolls", "/driver-pay-slabs", "/driver-advances"), false),
            new Module("reports", "Reports", List.of("/report-hub", "/reports", "/report-templates", "/scheduled-reports", "/exports"), false),
            new Module("settings", "Settings & Users", List.of("/users", "/roles", "/permissions", "/role-permissions", "/settings", "/branches", "/company"), false));

    /** Never managed here: platform admin (all access) and the fixed Driver App role. */
    public static final Set<String> NOT_MANAGED = Set.of("SUPER_ADMIN", "DRIVER");

    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired(required = false) private AuditService auditService;

    /** (companyId|role) -> module -> flags; empty map = no grid saved = full access. */
    private final Map<String, Map<String, boolean[]>> cache = new ConcurrentHashMap<>();

    /** The module an API path belongs to (longest prefix wins), or null when it is not managed. */
    public static Module moduleFor(String apiPath) {
        Module best = null; int len = -1;
        for (Module m : MODULES) for (String p : m.prefixes()) {
            if ((apiPath.equals(p) || apiPath.startsWith(p + "/")) && p.length() > len) { best = m; len = p.length(); }
        }
        return best;
    }

    /** GET = View (exports too); DELETE = Delete; PUT / PATCH = Edit; POST = Add, or Approve for approval-type actions. */
    public static Action actionFor(String method, String apiPath) {
        String m = method.toUpperCase(Locale.ROOT);
        if ("GET".equals(m) || apiPath.startsWith("/exports") || apiPath.endsWith("/pdf") || apiPath.endsWith("/print")) return Action.VIEW;
        if ("DELETE".equals(m)) return Action.DELETE;
        if ("PUT".equals(m) || "PATCH".equals(m)) return Action.EDIT;
        String last = apiPath.substring(apiPath.lastIndexOf('/') + 1);
        if (Set.of("approve", "reject", "post", "pay", "cancel", "close", "reverse", "unpost").contains(last)) return Action.APPROVE;
        if (Set.of("dispatch", "complete", "recalculate", "link", "allocate").contains(last)) return Action.EDIT;
        return Action.ADD;
    }

    private Map<String, boolean[]> grid(long companyId, String role) {
        return cache.computeIfAbsent(companyId + "|" + role, k -> {
            Map<String, boolean[]> g = new HashMap<>();
            for (Map<String, Object> r : jdbc.queryForList("""
                    SELECT module, can_view, can_add, can_edit, can_delete, can_approve FROM role_module_permissions
                     WHERE company_id = :c AND role_code = :r""", new MapSqlParameterSource("c", companyId).addValue("r", role))) {
                g.put((String) r.get("module"), new boolean[]{(Boolean) r.get("can_view"), (Boolean) r.get("can_add"),
                        (Boolean) r.get("can_edit"), (Boolean) r.get("can_delete"), (Boolean) r.get("can_approve")});
            }
            return g;
        });
    }

    /** True when ANY of the user's managed roles allows the action (a role without a saved grid allows everything). */
    public boolean allowed(long companyId, Collection<String> roles, Module module, Action action) {
        boolean anyManaged = false;
        for (String role : roles) {
            if (NOT_MANAGED.contains(role)) continue;
            anyManaged = true;
            Map<String, boolean[]> g = grid(companyId, role);
            if (g.isEmpty()) return true;
            boolean[] f = g.get(module.key());
            if (f == null || f[action.ordinal()]) return true;
        }
        return !anyManaged;
    }

    /** One role's grid for the screen (all true when none saved). */
    @Transactional(readOnly = true)
    public Map<String, Object> roleGrid(long companyId, String role) {
        Map<String, boolean[]> g = grid(companyId, role);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Module m : MODULES) {
            boolean[] f = g.getOrDefault(m.key(), new boolean[]{true, true, true, true, true});
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("module", m.key()); r.put("label", m.label()); r.put("listsOpen", m.listsOpen());
            r.put("view", f[0]); r.put("add", f[1]); r.put("edit", f[2]); r.put("delete", f[3]); r.put("approve", f[4]);
            rows.add(r);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", role); out.put("customized", !g.isEmpty()); out.put("modules", rows);
        return out;
    }

    /** Effective rights of a user (union over roles) — the screens hide menus / buttons with it. */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Boolean>> effective(long companyId, Collection<String> roles) {
        Map<String, Map<String, Boolean>> out = new LinkedHashMap<>();
        for (Module m : MODULES) {
            Map<String, Boolean> a = new LinkedHashMap<>();
            for (Action ac : Action.values()) a.put(ac.name().toLowerCase(Locale.ROOT), allowed(companyId, roles, m, ac));
            out.put(m.key(), a);
        }
        return out;
    }

    public record Row(String module, Boolean view, Boolean add, Boolean edit, Boolean delete, Boolean approve) {}

    @Transactional
    public Map<String, Object> save(long companyId, String role, List<Row> rows, String actor) {
        String r = check(role);
        Map<String, Row> byModule = new HashMap<>();
        for (Row x : rows == null ? List.<Row>of() : rows) if (x != null && x.module() != null) byModule.put(x.module(), x);
        for (String k : byModule.keySet()) if (MODULES.stream().noneMatch(m -> m.key().equals(k))) throw invalid("Unknown module: " + k);
        if (Set.of("COMPANY_ADMIN", "ADMIN").contains(r)) {   // never lock the company admin out of settings & users
            Row s = byModule.get("settings");
            if (s != null && (!Boolean.TRUE.equals(s.view()) || !Boolean.TRUE.equals(s.edit()) || !Boolean.TRUE.equals(s.add()))) {
                throw invalid("Company Admin must keep View, Add and Edit on Settings & Users (otherwise nobody can manage users).");
            }
        }
        jdbc.update("DELETE FROM role_module_permissions WHERE company_id = :c AND role_code = :r", new MapSqlParameterSource("c", companyId).addValue("r", r));
        for (Module m : MODULES) {
            Row x = byModule.getOrDefault(m.key(), new Row(m.key(), true, true, true, true, true));
            boolean view = !Boolean.FALSE.equals(x.view());
            jdbc.update("""
                    INSERT INTO role_module_permissions (company_id, role_code, module, can_view, can_add, can_edit, can_delete, can_approve, updated_by)
                    VALUES (:c, :r, :m, :v, :a, :e, :d, :p, :u)""", new MapSqlParameterSource("c", companyId).addValue("r", r).addValue("m", m.key())
                    .addValue("v", view).addValue("a", view && !Boolean.FALSE.equals(x.add())).addValue("e", view && !Boolean.FALSE.equals(x.edit()))
                    .addValue("d", view && !Boolean.FALSE.equals(x.delete())).addValue("p", view && !Boolean.FALSE.equals(x.approve())).addValue("u", actor));
        }
        cache.remove(companyId + "|" + r);
        if (auditService != null) auditService.log(actor, "ROLE_PERMISSIONS_SAVED", "role_module_permissions", companyId, null, "Permissions saved for role " + r);
        return roleGrid(companyId, r);
    }

    @Transactional
    public Map<String, Object> reset(long companyId, String role, String actor) {
        String r = check(role);
        jdbc.update("DELETE FROM role_module_permissions WHERE company_id = :c AND role_code = :r", new MapSqlParameterSource("c", companyId).addValue("r", r));
        cache.remove(companyId + "|" + r);
        if (auditService != null) auditService.log(actor, "ROLE_PERMISSIONS_RESET", "role_module_permissions", companyId, null, "Permissions reset to full for role " + r);
        return roleGrid(companyId, r);
    }

    /** Roles of this company that can be managed (company roles + global ones), without SUPER_ADMIN / DRIVER. */
    @Transactional(readOnly = true)
    public List<String> manageableRoles(long companyId) {
        return jdbc.queryForList("""
                SELECT DISTINCT code FROM app_roles WHERE is_deleted = false AND (company_id IS NULL OR company_id = :c)
                   AND code NOT IN ('SUPER_ADMIN', 'DRIVER') ORDER BY code""", new MapSqlParameterSource("c", companyId), String.class);
    }

    private static String check(String role) {
        String r = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (r.isEmpty() || NOT_MANAGED.contains(r)) throw invalid(r.isEmpty() ? "Choose a role." : r + " permissions are fixed and cannot be changed here.");
        return r;
    }

    private static BusinessValidationException invalid(String m) {
        return new BusinessValidationException("Permissions Not Saved", "ROLE_PERMISSIONS_INVALID", m, "Adjust the permissions and save again.");
    }
}
