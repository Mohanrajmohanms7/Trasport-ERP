package com.transport.erp.features;

import org.springframework.util.AntPathMatcher;

import java.util.*;

/**
 * Every client-facing module, inner tab and action of TransaFlow, with the screens (routes) and APIs it covers.
 * Single source of truth for subscription / client feature access (Platform Admin decides per plan and per client).
 *
 * Adding a module or tab later = add one line here (and, for a tab, hide it in the screen with FeatureService.has()).
 *
 * API rules: a request is governed by the most specific matching rule (longest pattern). For MASTER data the read (GET)
 * APIs stay open because other modules need them for dropdowns; disabling a master hides its screen and blocks changes.
 */
public final class FeatureCatalog {

    public enum Kind { MODULE, TAB, ACTION }

    public record ApiRule(String pattern, Set<String> methods) { }

    public record Feature(String code, String parent, String group, String label, String description, Kind kind,
                          boolean core, List<String> routes, String tabKey, List<ApiRule> api) { }

    private static final Set<String> ALL = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> WRITES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final List<Feature> FEATURES = new ArrayList<>();
    private static final Map<String, Feature> BY_CODE = new LinkedHashMap<>();
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static String currentGroup = "";

    /**
     * Optional add-ons: OFF unless the plan or the client switches them on (every other feature is ON unless switched off).
     * Children (tabs / actions) of an add-on follow their module, so only the module code is listed.
     */
    private static final Set<String> OPT_IN = Set.of("family-expenses");

    private static ApiRule all(String p) { return new ApiRule(p, ALL); }
    private static ApiRule writes(String p) { return new ApiRule(p, WRITES); }
    private static ApiRule only(String p, String... m) { return new ApiRule(p, Set.of(m)); }

    private static void group(String g) { currentGroup = g; }

    private static void module(String code, String label, String desc, boolean core, String route, ApiRule... api) {
        add(new Feature(code, null, currentGroup, label, desc, Kind.MODULE, core, route == null ? List.of() : List.of(route), null, List.of(api)));
    }

    private static void tab(String parent, String key, String label, String desc, ApiRule... api) {
        add(new Feature(parent + "." + key, parent, currentGroup, label, desc, Kind.TAB, false, List.of(), key, List.of(api)));
    }

    private static void action(String parent, String key, String label, String desc, ApiRule... api) {
        add(new Feature(parent + "." + key, parent, currentGroup, label, desc, Kind.ACTION, false, List.of(), key, List.of(api)));
    }

    /** Standard Add / Edit / Delete for a module whose collection API is {@code base}. */
    private static void crud(String parent, String base) {
        action(parent, "create", "Add", "Create new records", only(base, "POST"));
        action(parent, "edit", "Edit", "Change existing records", only(base + "/{id}", "PUT", "PATCH"), only(base + "/{id}/toggle-status", "PUT"));
        action(parent, "delete", "Delete", "Delete records", only(base + "/{id}", "DELETE"));
    }

    private static void add(Feature f) {
        FEATURES.add(f);
        BY_CODE.put(f.code(), f);
    }

    static {
        group("General");
        module("dashboard", "Dashboard", "Home dashboard and KPIs", true, "/dashboard", all("/api/v1/dashboard/**"));

        group("Masters");
        module("branches", "Branch Master", "Offices / yards and branch GSTIN", false, "/masters",
                writes("/api/v1/branches"), writes("/api/v1/branches/**"));
        module("customers", "Customer Master", "Customers, credit limit, GSTIN", false, "/customers",
                writes("/api/v1/customers"), writes("/api/v1/customers/**"));
        crud("customers", "/api/v1/customers");
        tab("customers", "sites", "Delivery sites", "Unloading sites per customer", writes("/api/v1/customers/*/delivery-sites/**"), writes("/api/v1/customers/*/delivery-sites"));
        tab("customers", "contacts", "Contact persons", "", all("/api/v1/customers/*/contacts/**"), all("/api/v1/customers/*/contacts"));
        tab("customers", "documents", "Customer documents", "", all("/api/v1/customers/*/documents/**"), all("/api/v1/customers/*/documents"));
        module("vehicles", "Vehicle Master", "Trucks, papers, expiry dates", false, "/vehicles",
                writes("/api/v1/vehicles"), writes("/api/v1/vehicles/**"));
        crud("vehicles", "/api/v1/vehicles");
        tab("vehicles", "documents", "Vehicle documents", "RC, insurance, permit…", all("/api/v1/vehicles/*/documents/**"), all("/api/v1/vehicles/*/documents"));
        tab("vehicles", "service-history", "Service history", "", all("/api/v1/vehicles/*/service-history/**"), all("/api/v1/vehicles/*/service-history"),
                all("/api/v1/vehicles/*/service-logs/**"), all("/api/v1/vehicles/*/service-logs"));
        tab("vehicles", "driver", "Driver assignment", "", all("/api/v1/vehicles/*/driver/**"), all("/api/v1/vehicles/*/driver"));
        tab("vehicles", "maintenance", "Preventive maintenance", "Service rules and odometer", all("/api/v1/maintenance/**"), all("/api/v1/vehicles/*/odometer/**"), all("/api/v1/vehicles/*/odometer"));
        module("drivers", "Driver Master", "Drivers and licences", false, "/drivers",
                writes("/api/v1/drivers"), writes("/api/v1/drivers/**"));
        crud("drivers", "/api/v1/drivers");
        tab("drivers", "documents", "Driver documents", "", all("/api/v1/drivers/*/documents/**"), all("/api/v1/drivers/*/documents"));
        tab("drivers", "attendance", "Attendance", "", all("/api/v1/drivers/*/attendance/**"), all("/api/v1/drivers/*/attendance"));
        tab("drivers", "salary", "Payroll config", "Basic salary / allowances", all("/api/v1/drivers/*/salary/**"), all("/api/v1/drivers/*/salary"));
        tab("drivers", "login", "Mobile login link", "", all("/api/v1/drivers/*/app-user"));
        module("materials", "Material & Quarry", "Materials, quarries, units, prices, loading points", false, "/materials-quarries",
                writes("/api/v1/materials/**"), writes("/api/v1/materials"));
        tab("materials", "materials", "Materials", "", writes("/api/v1/materials/**"), writes("/api/v1/materials"));
        tab("materials", "quarries", "Quarries", "", writes("/api/v1/quarries/**"), writes("/api/v1/quarries"));
        tab("materials", "uoms", "Units (UOM)", "", writes("/api/v1/uoms/**"), writes("/api/v1/uoms"));
        tab("materials", "conversions", "UOM conversions", "", writes("/api/v1/uoms/conversions/**"));
        tab("materials", "pricing", "Pricing", "", writes("/api/v1/material-prices/**"), writes("/api/v1/material-prices"));
        tab("materials", "locations", "Loading points", "", writes("/api/v1/loading-locations/**"), writes("/api/v1/loading-locations"));
        module("suppliers", "Supplier Master", "Workshops, parts shops, credit days", false, "/suppliers",
                writes("/api/v1/suppliers"), writes("/api/v1/suppliers/**"));
        crud("suppliers", "/api/v1/suppliers");

        group("Operations");
        module("bookings", "Bookings", "Customer orders", false, "/bookings", all("/api/v1/bookings/**"), all("/api/v1/bookings"));
        crud("bookings", "/api/v1/bookings");
        action("bookings", "approve", "Approve / reject / close", "", only("/api/v1/bookings/*/approve", "POST"), only("/api/v1/bookings/*/reject", "POST"), only("/api/v1/bookings/*/close", "POST"));
        module("trips", "Trips & Dispatch", "Trip planning, dispatch, delivery", false, "/trips-planning", all("/api/v1/trips/**"), all("/api/v1/trips"));
        crud("trips", "/api/v1/trips");
        action("trips", "dispatch", "Dispatch / complete", "", only("/api/v1/trips/*/dispatch", "POST"), only("/api/v1/trips/*/complete", "POST"), only("/api/v1/trips/*/cancel", "POST"));
        tab("trips", "pod", "POD / trip documents", "", all("/api/v1/trips/*/documents/**"), all("/api/v1/trips/*/documents"));
        tab("trips", "gps", "GPS tracking", "", all("/api/v1/gps/**"));
        module("fuel", "Fuel", "Fuel entries and requests", false, "/fuel-logs", all("/api/v1/fuel/**"), all("/api/v1/fuel"));
        tab("fuel", "entries", "Fuel entries", "", all("/api/v1/fuel/**"), all("/api/v1/fuel"));
        tab("fuel", "requests", "Fuel requests", "", all("/api/v1/fuel/request/**"), all("/api/v1/fuel/request"));
        module("expenses", "Expenses", "Expense vouchers", false, "/expense-logs", all("/api/v1/expenses/**"), all("/api/v1/expenses"));
        crud("expenses", "/api/v1/expenses");

        group("Personal");
        // Optional add-on (off by default): owner's family expenses, kept apart from the business. docs/FAMILY_EXPENSES.md
        module("family-expenses", "Family Expenses", "Owner's family / personal expenses, kept apart from the business (add-on, off by default)",
                false, "/family-expenses", all("/api/v1/family-expenses/**"), all("/api/v1/family-expenses"));
        crud("family-expenses", "/api/v1/family-expenses");
        tab("family-expenses", "reports", "Reports", "Daily, monthly, category, payment mode, yearly, comparison",
                only("/api/v1/family-expenses/reports/**", "GET"));
        action("family-expenses", "export", "Export", "Excel / PDF", only("/api/v1/family-expenses/export/**", "GET"));
        tab("family-expenses", "categories", "Categories", "Family expense categories",
                writes("/api/v1/family-expenses/categories/**"), writes("/api/v1/family-expenses/categories"));

        group("Maintenance & Stores");
        module("maintenance-requests", "Maintenance Requests", "Problems reported by drivers / staff", false, "/maintenance-requests",
                all("/api/v1/maintenance-requests/**"), all("/api/v1/maintenance-requests"));
        module("work-orders", "Work Orders", "Repair jobs, parts and labour", false, "/work-orders",
                all("/api/v1/work-orders/**"), all("/api/v1/work-orders"));
        module("spare-parts", "Spare Parts", "Parts catalogue", false, "/spare-parts", writes("/api/v1/spare-parts/**"), writes("/api/v1/spare-parts"));
        module("warehouses", "Warehouses", "Stores", false, "/inventory/warehouses", writes("/api/v1/warehouses/**"), writes("/api/v1/warehouses"));
        module("stock", "Stock", "Stock balances, opening stock, receipts, issues", false, "/inventory/stock",
                all("/api/v1/inventory/stock/**"), all("/api/v1/inventory/stock"));
        tab("stock", "opening", "Opening stock", "", only("/api/v1/inventory/stock/opening-balance", "POST"), only("/api/v1/inventory/stock/*/initial-cost", "POST"));
        tab("stock", "receipt", "Receive stock", "Purchases into stock", only("/api/v1/inventory/stock/receipt", "POST"));
        module("inventory-transactions", "Inventory Transactions", "Stock movement history", false, "/inventory/transactions",
                all("/api/v1/inventory/transactions/**"), all("/api/v1/inventory/transactions"));

        group("Finance");
        module("invoices", "Invoices", "GST invoices", false, "/billing-invoices",
                all("/api/v1/invoices/**"), all("/api/v1/invoices"), all("/api/v1/sales-invoices/**"), all("/api/v1/sales-invoices"));
        tab("invoices", "ledger", "Invoice register", "");
        tab("invoices", "ready", "Ready for billing", "Create invoices from completed trips",
                only("/api/v1/invoices/from-trip/**", "POST"), only("/api/v1/invoices/from-trips", "POST"));
        action("invoices", "approve", "Approve / cancel", "", only("/api/v1/invoices/*/approve", "POST"), only("/api/v1/invoices/*/cancel", "POST"));
        module("receipts", "Customer Receipts", "Money received from customers", false, "/payment-logs",
                all("/api/v1/receipts/**"), all("/api/v1/receipts"), all("/api/v1/customer-ledger/**"));
        tab("receipts", "receipts", "Receipts", "");
        tab("receipts", "ledger", "Customer ledger", "", all("/api/v1/customer-ledger/**"));
        tab("receipts", "dashboard", "Settlement dashboard", "", only("/api/v1/receipts/dashboard", "GET"), only("/api/v1/receipts/customers/*/apply-advance", "POST"),
                only("/api/v1/receipts/*/apply-advance", "POST"));
        module("payables", "Supplier Bills & Payments", "Accounts payable", false, "/payables", all("/api/v1/payables/**"));
        tab("payables", "bills", "Supplier bills", "", all("/api/v1/payables/bills/**"), all("/api/v1/payables/bills"));
        tab("payables", "payments", "Supplier payments", "", all("/api/v1/payables/payments/**"), all("/api/v1/payables/payments"));
        module("payroll", "Driver Payroll", "Daily-slab salary, advances", false, "/driver-payroll",
                all("/api/v1/driver-payrolls/**"), all("/api/v1/driver-payrolls"));
        tab("payroll", "payrolls", "Payrolls", "", all("/api/v1/driver-payrolls/**"));
        tab("payroll", "advances", "Driver advances", "", all("/api/v1/driver-advances/**"), all("/api/v1/driver-advances"));
        tab("payroll", "slabs", "Daily pay slabs", "", all("/api/v1/driver-pay-slabs/**"), all("/api/v1/driver-pay-slabs"));
        module("accounts", "Accounts", "Journals and chart of accounts", false, "/accounts-ledger",
                all("/api/v1/journal/**"), all("/api/v1/journal"), writes("/api/v1/accounts/**"), writes("/api/v1/accounts"));
        tab("accounts", "journal", "Journal / day book", "", all("/api/v1/journal/**"), all("/api/v1/journal"));
        tab("accounts", "chart", "Chart of accounts", "", writes("/api/v1/accounts/**"), writes("/api/v1/accounts"));
        tab("accounts", "trial", "Trial balance", "");

        group("Reports");
        module("reports", "Reports", "40 business reports", false, "/reports", all("/api/v1/report-hub/**"));
        for (String[] c : new String[][]{{"monthly", "Monthly & management"}, {"operations", "Operations"}, {"vehicles", "Vehicles & trips"},
                {"bookings", "Bookings & delivery"}, {"sales", "Invoices & receipts"}, {"outstanding", "Outstanding & pending"},
                {"drivers", "Drivers & settlement"}, {"fuel", "Fuel & expenses"}, {"maintenance", "Maintenance & work orders"},
                {"stock", "Warehouse & stock"}, {"accounting", "Financial & accounting"}}) {
            tab("reports", c[0], c[1], "Report category");
        }
        module("financial-statements", "Financial Statements", "P&L, balance sheet, trial balance, report templates", false, "/reports-bi",
                all("/api/v1/financial-reports/**"), all("/api/v1/reports/**"));

        group("Tools");
        module("export", "Excel / PDF export", "Export buttons on list screens", false, null, all("/api/v1/exports/**"));
        module("bulk-upload", "Excel bulk upload", "Upload masters / opening stock from Excel", false, null, all("/api/v1/bulk-import/**"));
        module("attachments", "Attachments & photos", "Documents and photos on records", false, null,
                writes("/api/v1/attachments/**"), writes("/api/v1/attachments"), writes("/api/v1/photos/**"));
        module("ai", "Mobility & AI insights", "AI predictions screen", false, "/mobility-ai", all("/api/v1/ai/**"));

        group("Admin");
        module("users", "Users & Roles", "Logins and roles (always available)", true, "/users-roles", all("/api/v1/users/**"), all("/api/v1/roles/**"));
        module("dropdowns", "Dropdown Lists", "Vehicle types, categories, units…", false, "/lookup-values", writes("/api/v1/lookups/**"), writes("/api/v1/lookups"));
        module("settings", "System Settings", "Company profile, financial years, settings (always available)", true, "/company-admin",
                all("/api/v1/settings/**"), all("/api/v1/financial-years/**"));
    }

    /** Feature → features it cannot work without (a plan can never be half-configured). */
    private static final Map<String, List<String>> REQUIRES = Map.ofEntries(
            Map.entry("payables", List.of("suppliers")),
            Map.entry("stock", List.of("spare-parts", "warehouses")),
            Map.entry("inventory-transactions", List.of("stock")),
            Map.entry("financial-statements", List.of("accounts")),
            Map.entry("maintenance-requests", List.of("work-orders")),
            Map.entry("reports.stock", List.of("stock")),
            Map.entry("reports.accounting", List.of("accounts")),
            Map.entry("reports.maintenance", List.of("work-orders")),
            Map.entry("reports.drivers", List.of("payroll")),
            Map.entry("drivers.salary", List.of("payroll")));

    public static List<String> requires(String code) { return REQUIRES.getOrDefault(code, List.of()); }

    private FeatureCatalog() { }

    public static List<Feature> all() { return Collections.unmodifiableList(FEATURES); }

    public static Feature get(String code) { return BY_CODE.get(code); }

    public static boolean exists(String code) { return BY_CODE.containsKey(code); }

    /** Value used when neither the plan nor the client has a setting: true, except for opt-in add-ons. */
    public static boolean defaultOn(String code) { return !OPT_IN.contains(code); }

    /** Feature governing this request, or null when the request is not feature-controlled. Most specific pattern wins. */
    public static Feature match(String path, String method) {
        Feature best = null;
        int bestScore = -1;
        for (Feature f : FEATURES) {
            for (ApiRule r : f.api()) {
                if (!r.methods().contains(method) || !MATCHER.match(r.pattern(), path)) continue;
                int score = specificity(r.pattern()) * 10 + (f.kind() == Kind.MODULE ? 0 : 1);
                if (score > bestScore) { best = f; bestScore = score; }
            }
        }
        return best;
    }

    private static int specificity(String pattern) {
        int score = pattern.length() * 4;
        score -= countOf(pattern, "**") * 40;
        score -= countOf(pattern, "*") * 10;
        score -= countOf(pattern, "{") * 10;
        return score;
    }

    private static int countOf(String s, String token) {
        int n = 0;
        for (int i = s.indexOf(token); i >= 0; i = s.indexOf(token, i + token.length())) n++;
        return n;
    }

    /** Report hub category title → feature code. */
    public static String reportFeature(String categoryTitle) {
        if (categoryTitle == null) return "reports";
        for (Feature f : FEATURES) {
            if ("reports".equals(f.parent()) && f.label().equalsIgnoreCase(categoryTitle)) return f.code();
        }
        return "reports";
    }
}
