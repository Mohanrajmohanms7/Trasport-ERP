package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.FamilyExpense;
import com.transport.erp.model.LookupValue;
import com.transport.erp.repository.FamilyExpenseRepository;
import com.transport.erp.repository.LookupValueRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Family Expenses — the owner's personal expense register (optional add-on). See docs/FAMILY_EXPENSES.md.
 *
 * <p>Deliberately separate from the business: its own table, no vehicle / driver / trip links, no journal entries,
 * no cash / bank / P&L effect, not part of any business report or the dashboard. Only company admins of the
 * signed-in company can use it (the controller enforces the roles; the platform admin cannot read it).</p>
 */
@Service
public class FamilyExpenseService {

    public static final String CATEGORY_TYPE = "FAMILY_EXPENSE_CATEGORY";
    public static final String MODE_TYPE = "PAYMENT_METHOD";
    public static final String DOC_TYPE = "FAMILY_EXPENSE";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /** Starting categories, created the first time a company opens the module. The company can rename / add / deactivate. */
    private static final String[][] DEFAULT_CATEGORIES = {
            {"GROCERIES", "Food / Groceries"}, {"EDUCATION", "Education"}, {"MEDICAL", "Medical"}, {"HOUSE_RENT", "House Rent"},
            {"ELECTRICITY", "Electricity"}, {"TRAVEL", "Travel"}, {"SHOPPING", "Shopping"}, {"HOUSEHOLD", "Household"},
            {"INSURANCE", "Insurance"}, {"OTHER", "Other"}};
    /** Used only if a company has no payment-method dropdown values at all. CREDIT is never offered here. */
    private static final String[][] FALLBACK_MODES = {{"CASH", "Cash"}, {"UPI", "UPI"}, {"BANK_TRANSFER", "Bank Transfer"}, {"CHEQUE", "Cheque"}};

    @Autowired private FamilyExpenseRepository repository;
    @Autowired private LookupValueRepository lookupRepository;
    @Autowired private DocumentNumberService documentNumberService;
    @Autowired private TenantAccessService tenantAccess;
    @Autowired private AuditService auditService;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private XlsxExportService exports;

    /** Always the signed-in user's own company (never a company id from the request). */
    private Long companyId() {
        Long cid = tenantAccess.resolveCompanyId(null);
        if (cid == null) throw new AccessDeniedException("Family expenses belong to a company login.");
        return cid;
    }

    // =====================================================================================================  options

    /** Active categories and payment modes for the entry form and filters. */
    @Transactional
    public Map<String, Object> options() {
        Long cid = companyId();
        ensureCategories(cid);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("categories", lookupRows(cid, CATEGORY_TYPE, true));
        out.put("paymentModes", modes(cid, true));
        return out;
    }

    private void ensureCategories(Long cid) {
        if (!lookupRepository.findByCompanyIdAndTypeAndIsDeletedFalse(cid, CATEGORY_TYPE).isEmpty()) return;
        for (String[] c : DEFAULT_CATEGORIES) {
            LookupValue v = new LookupValue();
            v.setType(CATEGORY_TYPE);
            v.setCode(c[0]);
            v.setName(c[1]);
            v.setDescription(c[1]);
            v.setStatus("ACTIVE");
            v.setCompanyId(cid);
            v.setIsDeleted(false);
            v.setCreatedBy("SYSTEM");
            v.setUpdatedBy("SYSTEM");
            lookupRepository.save(v);
        }
    }

    private List<Map<String, Object>> lookupRows(Long cid, String type, boolean activeOnly) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<LookupValue> rows = new ArrayList<>(lookupRepository.findByCompanyIdAndTypeAndIsDeletedFalse(cid, type));
        rows.sort(Comparator.comparing((LookupValue v) -> "OTHER".equals(v.getCode())).thenComparing(v -> String.valueOf(v.getName())));
        for (LookupValue v : rows) {
            boolean active = v.getStatus() == null || "ACTIVE".equalsIgnoreCase(v.getStatus());
            if (activeOnly && !active) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("code", v.getCode());
            m.put("name", v.getName());
            m.put("status", active ? "ACTIVE" : "INACTIVE");
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> modes(Long cid, boolean activeOnly) {
        List<Map<String, Object>> rows = lookupRows(cid, MODE_TYPE, activeOnly);
        rows.removeIf(m -> "CREDIT".equalsIgnoreCase(String.valueOf(m.get("code"))));
        if (rows.isEmpty()) {
            for (String[] m : FALLBACK_MODES) rows.add(new LinkedHashMap<>(Map.of("code", m[0], "name", m[1], "status", "ACTIVE")));
        }
        return rows;
    }

    private Map<String, String> names(Long cid, String type) {
        Map<String, String> m = new HashMap<>();
        for (LookupValue v : lookupRepository.findByCompanyIdAndTypeAndIsDeletedFalse(cid, type)) m.put(v.getCode(), v.getName());
        if (MODE_TYPE.equals(type)) for (String[] f : FALLBACK_MODES) m.putIfAbsent(f[0], f[1]);
        return m;
    }

    // =====================================================================================================  categories

    @Transactional
    public List<Map<String, Object>> categories() {
        Long cid = companyId();
        ensureCategories(cid);
        List<Map<String, Object>> rows = lookupRows(cid, CATEGORY_TYPE, false);
        for (Map<String, Object> r : rows) {
            r.put("used", repository.countByCompanyIdAndCategoryAndIsDeletedFalse(cid, String.valueOf(r.get("code"))));
        }
        return rows;
    }

    @Transactional
    public List<Map<String, Object>> createCategory(Map<String, Object> body, String user) {
        Long cid = companyId();
        ensureCategories(cid);
        String name = cleanName(body.get("name"));
        String base = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "CATEGORY";
        if (base.length() > 40) base = base.substring(0, 40);
        String code = base;
        for (int i = 2; lookupRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(cid, CATEGORY_TYPE, code).isPresent(); i++) code = base + "_" + i;
        assertNameFree(cid, name, null);
        LookupValue v = new LookupValue();
        v.setType(CATEGORY_TYPE);
        v.setCode(code);
        v.setName(name);
        v.setDescription(name);
        v.setStatus("ACTIVE");
        v.setCompanyId(cid);
        v.setIsDeleted(false);
        v.setCreatedBy(user);
        v.setUpdatedBy(user);
        lookupRepository.save(v);
        return categories();
    }

    @Transactional
    public List<Map<String, Object>> updateCategory(Long id, Map<String, Object> body, String user) {
        Long cid = companyId();
        LookupValue v = ownCategory(cid, id);
        if (body.containsKey("name")) {
            String name = cleanName(body.get("name"));
            assertNameFree(cid, name, v.getId());
            v.setName(name);
            v.setDescription(name);
        }
        if (body.get("status") != null) {
            String st = String.valueOf(body.get("status")).toUpperCase(Locale.ROOT);
            if (!st.equals("ACTIVE") && !st.equals("INACTIVE")) {
                throw new BusinessValidationException("Invalid Status", "FAMILY_CATEGORY_STATUS", "Status must be ACTIVE or INACTIVE.", "Pick Active or Inactive.");
            }
            v.setStatus(st);
        }
        v.setUpdatedBy(user);
        lookupRepository.save(v);
        return categories();
    }

    @Transactional
    public List<Map<String, Object>> deleteCategory(Long id, String user) {
        Long cid = companyId();
        LookupValue v = ownCategory(cid, id);
        long used = repository.countByCompanyIdAndCategoryAndIsDeletedFalse(cid, v.getCode());
        if (used > 0) {
            throw new BusinessValidationException("Category In Use", "FAMILY_CATEGORY_IN_USE",
                    "\"" + v.getName() + "\" is used by " + used + " expense(s) and cannot be deleted.",
                    "Make it Inactive instead — old expenses keep it, new ones cannot use it.");
        }
        v.setIsDeleted(true);
        v.setUpdatedBy(user);
        lookupRepository.save(v);
        return categories();
    }

    private LookupValue ownCategory(Long cid, Long id) {
        LookupValue v = lookupRepository.findById(id).filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException("Category Not Found", "FAMILY_CATEGORY_NOT_FOUND",
                        "This category does not exist.", "Refresh the page."));
        if (!CATEGORY_TYPE.equals(v.getType()) || !cid.equals(v.getCompanyId())) throw new AccessDeniedException("Access denied");
        return v;
    }

    private static String cleanName(Object raw) {
        String name = raw == null ? "" : String.valueOf(raw).trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > 60) {
            throw new BusinessValidationException("Category Name", "FAMILY_CATEGORY_NAME", "Enter a category name of 1 to 60 characters.", "Example: Festival, Gifts.");
        }
        return name;
    }

    private void assertNameFree(Long cid, String name, Long exceptId) {
        boolean taken = lookupRepository.findByCompanyIdAndTypeAndIsDeletedFalse(cid, CATEGORY_TYPE).stream()
                .anyMatch(x -> name.equalsIgnoreCase(x.getName()) && !x.getId().equals(exceptId));
        if (taken) {
            throw new BusinessValidationException("Category Exists", "FAMILY_CATEGORY_DUPLICATE", "A category named \"" + name + "\" already exists.", "Use the existing category.");
        }
    }

    // =====================================================================================================  register

    @Transactional(readOnly = true)
    public Map<String, Object> list(LocalDate from, LocalDate to, String category, String mode, int page, int size) {
        Long cid = companyId();
        LocalDate[] p = period(from, to);
        Page<FamilyExpense> result = repository.search(cid, p[0], p[1], blank(category), blank(mode),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 500)));
        Map<String, String> cats = names(cid, CATEGORY_TYPE), modes = names(cid, MODE_TYPE);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FamilyExpense f : result.getContent()) rows.add(view(f, cats, modes));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", rows);
        out.put("totalElements", result.getTotalElements());
        out.put("page", result.getNumber());
        out.put("size", result.getSize());
        out.put("totalAmount", jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM family_expenses WHERE company_id = :cid AND is_deleted = false"
                + " AND expense_date BETWEEN :from AND :to AND (CAST(:cat AS VARCHAR) IS NULL OR category = :cat) AND (CAST(:mode AS VARCHAR) IS NULL OR payment_mode = :mode)",
                params(cid, p[0], p[1]).addValue("cat", blank(category)).addValue("mode", blank(mode)), BigDecimal.class));
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        Long cid = companyId();
        return view(own(cid, id), names(cid, CATEGORY_TYPE), names(cid, MODE_TYPE));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, String user) {
        Long cid = companyId();
        ensureCategories(cid);
        FamilyExpense f = new FamilyExpense();
        f.setCompanyId(cid);
        apply(cid, f, body, null);
        f.setExpenseNumber(documentNumberService.next(cid, DOC_TYPE, "FX-", f.getExpenseDate()));
        f.setCode(f.getExpenseNumber());
        f.setStatus("ACTIVE");
        f.setIsDeleted(false);
        f.setCreatedBy(user);
        f.setUpdatedBy(user);
        FamilyExpense saved = repository.save(f);
        // Audit text stays generic (no amount / description) — audit logs are also seen by the platform operator.
        auditService.log(user, "FAMILY_EXPENSE_CREATED", "family_expenses", saved.getId(), null, "Family expense " + saved.getExpenseNumber() + " added");
        return view(saved, names(cid, CATEGORY_TYPE), names(cid, MODE_TYPE));
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body, String user) {
        Long cid = companyId();
        FamilyExpense f = own(cid, id);
        apply(cid, f, body, f);
        f.setUpdatedBy(user);
        FamilyExpense saved = repository.save(f);
        auditService.log(user, "FAMILY_EXPENSE_UPDATED", "family_expenses", saved.getId(), null, "Family expense " + saved.getExpenseNumber() + " changed");
        return view(saved, names(cid, CATEGORY_TYPE), names(cid, MODE_TYPE));
    }

    @Transactional
    public void delete(Long id, String user) {
        Long cid = companyId();
        FamilyExpense f = own(cid, id);
        f.setIsDeleted(true);
        f.setUpdatedBy(user);
        repository.save(f);
        auditService.log(user, "FAMILY_EXPENSE_DELETED", "family_expenses", f.getId(), null, "Family expense " + f.getExpenseNumber() + " deleted");
    }

    private FamilyExpense own(Long cid, Long id) {
        FamilyExpense f = repository.findById(id).filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException("Expense Not Found", "FAMILY_EXPENSE_NOT_FOUND",
                        "This family expense does not exist or was deleted.", "Refresh the list."));
        if (!cid.equals(f.getCompanyId())) throw new AccessDeniedException("Access denied to another company's family expense");
        return f;
    }

    /** Validates and copies the editable fields. An existing entry may keep a category / mode that was later switched off. */
    private void apply(Long cid, FamilyExpense f, Map<String, Object> b, FamilyExpense previous) {
        List<String> errors = new ArrayList<>();
        LocalDate date = null;
        try {
            date = b.get("expenseDate") == null || String.valueOf(b.get("expenseDate")).isBlank() ? null : LocalDate.parse(String.valueOf(b.get("expenseDate")).substring(0, 10));
        } catch (Exception e) { errors.add("Date is not valid (use YYYY-MM-DD)."); }
        if (date == null && errors.isEmpty()) errors.add("Date is required.");
        if (date != null && date.isAfter(LocalDate.now())) errors.add("Date cannot be in the future.");

        BigDecimal amount = null;
        try {
            amount = b.get("amount") == null || String.valueOf(b.get("amount")).isBlank() ? null : new BigDecimal(String.valueOf(b.get("amount"))).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) { errors.add("Amount must be a number."); }
        if (amount == null && errors.stream().noneMatch(x -> x.startsWith("Amount"))) errors.add("Amount is required.");
        if (amount != null && (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0)) errors.add("Amount must be more than ₹0.");

        String category = str(b.get("category"));
        boolean keepCat = previous != null && category != null && category.equals(previous.getCategory());
        if (category == null) errors.add("Category is required.");
        else if (!keepCat && lookupRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(cid, CATEGORY_TYPE, category)
                .filter(v -> v.getStatus() == null || "ACTIVE".equalsIgnoreCase(v.getStatus())).isEmpty()) {
            errors.add("Pick an active category from the list.");
        }

        String mode = str(b.get("paymentMode"));
        boolean keepMode = previous != null && mode != null && mode.equals(previous.getPaymentMode());
        if (mode == null) errors.add("Payment mode is required.");
        else if (!keepMode && modes(cid, true).stream().noneMatch(m -> mode.equals(m.get("code")))) errors.add("Pick a payment mode from the list.");

        String desc = str(b.get("description")), member = str(b.get("memberName")), ref = str(b.get("referenceNo"));
        if (desc != null && desc.length() > 500) errors.add("Description can be at most 500 characters.");
        if (member != null && member.length() > 100) errors.add("Family member can be at most 100 characters.");
        if (ref != null && ref.length() > 100) errors.add("Reference can be at most 100 characters.");

        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Check The Expense", "FAMILY_EXPENSE_INVALID", errors.get(0), "Correct the highlighted details and save again.", errors);
        }
        f.setExpenseDate(date);
        f.setAmount(amount);
        f.setCategory(category);
        f.setPaymentMode(mode);
        f.setDescription(desc);
        f.setMemberName(member);
        f.setReferenceNo(ref);
        String catName = names(cid, CATEGORY_TYPE).getOrDefault(category, category);
        f.setName(catName.length() > 150 ? catName.substring(0, 150) : catName);
    }

    private static Map<String, Object> view(FamilyExpense f, Map<String, String> cats, Map<String, String> modes) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("expenseNumber", f.getExpenseNumber());
        m.put("expenseDate", f.getExpenseDate());
        m.put("category", f.getCategory());
        m.put("categoryName", cats.getOrDefault(f.getCategory(), f.getCategory()));
        m.put("amount", f.getAmount());
        m.put("paymentMode", f.getPaymentMode());
        m.put("paymentModeName", modes.getOrDefault(f.getPaymentMode(), f.getPaymentMode()));
        m.put("description", f.getDescription());
        m.put("memberName", f.getMemberName());
        m.put("referenceNo", f.getReferenceNo());
        m.put("createdBy", f.getCreatedBy());
        m.put("updatedDate", f.getUpdatedDate());
        return m;
    }

    // =====================================================================================================  summary

    /** Month header: total, cash, bank/UPI, entries, highest category, previous month total. */
    @Transactional(readOnly = true)
    public Map<String, Object> summary(String month) {
        Long cid = companyId();
        YearMonth ym;
        try { ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month); }
        catch (Exception e) { ym = YearMonth.now(); }
        MapSqlParameterSource p = params(cid, ym.atDay(1), ym.atEndOfMonth());
        Map<String, Object> t = jdbc.queryForMap("SELECT COUNT(*) entries, COALESCE(SUM(amount),0) total,"
                + " COALESCE(SUM(amount) FILTER (WHERE payment_mode = 'CASH'),0) cash, COALESCE(SUM(amount) FILTER (WHERE payment_mode <> 'CASH'),0) bank"
                + " FROM family_expenses WHERE company_id = :cid AND is_deleted = false AND expense_date BETWEEN :from AND :to", p);
        List<Map<String, Object>> top = jdbc.queryForList("SELECT category, SUM(amount) amount FROM family_expenses"
                + " WHERE company_id = :cid AND is_deleted = false AND expense_date BETWEEN :from AND :to GROUP BY category ORDER BY amount DESC, category LIMIT 1", p);
        BigDecimal prev = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM family_expenses WHERE company_id = :cid AND is_deleted = false AND expense_date BETWEEN :from AND :to",
                params(cid, ym.minusMonths(1).atDay(1), ym.minusMonths(1).atEndOfMonth()), BigDecimal.class);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", ym.toString());
        out.put("label", ym.format(DateTimeFormatter.ofPattern("MMM yyyy")));
        out.put("entries", ((Number) t.get("entries")).longValue());
        out.put("total", t.get("total"));
        out.put("cash", t.get("cash"));
        out.put("bank", t.get("bank"));
        out.put("previousMonthTotal", prev);
        if (!top.isEmpty()) {
            String code = String.valueOf(top.get(0).get("category"));
            out.put("topCategory", Map.of("code", code, "name", names(cid, CATEGORY_TYPE).getOrDefault(code, code), "amount", top.get(0).get("amount")));
        } else {
            out.put("topCategory", null);
        }
        return out;
    }

    // =====================================================================================================  reports

    public record Report(String key, String title, String period, List<Map<String, Object>> columns, List<Map<String, Object>> rows, Map<String, Object> totals) { }

    private static final String CAT_JOIN = " LEFT JOIN lookup_values c ON c.company_id = f.company_id AND c.type = 'FAMILY_EXPENSE_CATEGORY' AND c.code = f.category AND c.is_deleted = false";
    private static final String MODE_JOIN = " LEFT JOIN lookup_values pm ON pm.company_id = f.company_id AND pm.type = 'PAYMENT_METHOD' AND pm.code = f.payment_mode AND pm.is_deleted = false";
    private static final String BASE = " FROM family_expenses f" + CAT_JOIN + MODE_JOIN
            + " WHERE f.company_id = :cid AND f.is_deleted = false AND f.expense_date BETWEEN :from AND :to"
            + " AND (CAST(:cat AS VARCHAR) IS NULL OR f.category = :cat) AND (CAST(:mode AS VARCHAR) IS NULL OR f.payment_mode = :mode)";
    private static final String SPLIT = "COUNT(*) entries, SUM(f.amount) total, COALESCE(SUM(f.amount) FILTER (WHERE f.payment_mode = 'CASH'),0) cash,"
            + " COALESCE(SUM(f.amount) FILTER (WHERE f.payment_mode <> 'CASH'),0) bank";

    /** register | daily | monthly | category | mode | yearly | comparison. */
    @Transactional(readOnly = true)
    public Report report(String key, LocalDate from, LocalDate to, String category, String mode) {
        Long cid = companyId();
        LocalDate[] p = period(from, to);
        MapSqlParameterSource ps = params(cid, p[0], p[1]).addValue("cat", blank(category)).addValue("mode", blank(mode));
        String period = p[0].format(D) + " to " + p[1].format(D);
        List<Map<String, Object>> cols;
        List<Map<String, Object>> rows;
        String title;
        switch (key == null ? "" : key) {
            case "register" -> {
                title = "Family expenses";
                cols = cols("expense_date:Date:date", "expense_number:No:text", "category:Category:text", "description:Description:text",
                        "member:Member:text", "mode:Payment mode:text", "reference:Reference:text", "amount:Amount:money");
                rows = jdbc.queryForList("SELECT f.expense_date, f.expense_number, COALESCE(c.name, f.category) category, f.description, f.member_name member,"
                        + " COALESCE(pm.name, f.payment_mode) mode, f.reference_no reference, f.amount" + BASE + " ORDER BY f.expense_date, f.id", ps);
            }
            case "daily" -> {
                title = "Daily family expenses";
                cols = cols("day:Date:date", "entries:Entries:number", "cash:Cash:money", "bank:Bank / UPI:money", "total:Total:money");
                rows = jdbc.queryForList("SELECT f.expense_date AS day, " + SPLIT + BASE + " GROUP BY f.expense_date ORDER BY f.expense_date", ps);
            }
            case "monthly" -> {
                title = "Monthly family expenses";
                cols = cols("month:Month:text", "entries:Entries:number", "cash:Cash:money", "bank:Bank / UPI:money", "total:Total:money");
                rows = jdbc.queryForList("SELECT to_char(date_trunc('month', f.expense_date), 'Mon YYYY') AS month, " + SPLIT + BASE
                        + " GROUP BY date_trunc('month', f.expense_date) ORDER BY date_trunc('month', f.expense_date)", ps);
            }
            case "category" -> {
                title = "Category-wise family expenses";
                cols = cols("category:Category:text", "entries:Entries:number", "total:Total:money", "share:Share %:percent");
                rows = jdbc.queryForList("SELECT COALESCE(MAX(c.name), f.category) category, COUNT(*) entries, SUM(f.amount) total,"
                        + " ROUND(100 * SUM(f.amount) / NULLIF(SUM(SUM(f.amount)) OVER (), 0), 1) share" + BASE + " GROUP BY f.category ORDER BY total DESC", ps);
            }
            case "mode" -> {
                title = "Payment-mode-wise family expenses";
                cols = cols("mode:Payment mode:text", "entries:Entries:number", "total:Total:money", "share:Share %:percent");
                rows = jdbc.queryForList("SELECT COALESCE(MAX(pm.name), f.payment_mode) mode, COUNT(*) entries, SUM(f.amount) total,"
                        + " ROUND(100 * SUM(f.amount) / NULLIF(SUM(SUM(f.amount)) OVER (), 0), 1) share" + BASE + " GROUP BY f.payment_mode ORDER BY total DESC", ps);
            }
            case "yearly" -> {
                // Financial years (April–March), all years on record.
                title = "Yearly family expenses (financial year)";
                period = "All years";
                cols = cols("year:Financial year:text", "entries:Entries:number", "cash:Cash:money", "bank:Bank / UPI:money", "total:Total:money");
                MapSqlParameterSource all = params(cid, LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31)).addValue("cat", blank(category)).addValue("mode", blank(mode));
                rows = jdbc.queryForList("SELECT x.fy || '-' || LPAD(CAST(MOD(x.fy + 1, 100) AS VARCHAR), 2, '0') AS year, SUM(x.entries) entries, SUM(x.cash) cash, SUM(x.bank) bank, SUM(x.total) total"
                        + " FROM (SELECT CAST(EXTRACT(YEAR FROM f.expense_date) AS INT) - CASE WHEN EXTRACT(MONTH FROM f.expense_date) < 4 THEN 1 ELSE 0 END AS fy, " + SPLIT + BASE
                        + " GROUP BY 1) x GROUP BY x.fy ORDER BY x.fy", all);
            }
            case "comparison" -> {
                // Category × month for the financial year that contains "from" (or today).
                LocalDate anchor = from != null ? from : LocalDate.now();
                int fy = anchor.getMonthValue() >= 4 ? anchor.getYear() : anchor.getYear() - 1;
                LocalDate s = LocalDate.of(fy, 4, 1), e = LocalDate.of(fy + 1, 3, 31);
                title = "Category comparison by month — FY " + fy + "-" + String.format("%02d", (fy + 1) % 100);
                period = s.format(D) + " to " + e.format(D);
                List<String> keys = new ArrayList<>();
                List<String> spec = new ArrayList<>(List.of("category:Category:text"));
                for (int i = 0; i < 12; i++) {
                    YearMonth m = YearMonth.from(s).plusMonths(i);
                    keys.add("m" + i);
                    spec.add("m" + i + ":" + m.format(DateTimeFormatter.ofPattern("MMM yy")) + ":money");
                }
                spec.add("total:Total:money");
                cols = cols(spec.toArray(new String[0]));
                List<Map<String, Object>> raw = jdbc.queryForList("SELECT f.category code, COALESCE(MAX(c.name), f.category) category,"
                        + " (EXTRACT(YEAR FROM f.expense_date) - :fy) * 12 + EXTRACT(MONTH FROM f.expense_date) - 4 AS idx, SUM(f.amount) amount" + BASE
                        + " GROUP BY f.category, EXTRACT(YEAR FROM f.expense_date), EXTRACT(MONTH FROM f.expense_date)",
                        params(cid, s, e).addValue("cat", blank(category)).addValue("mode", blank(mode)).addValue("fy", fy));
                LinkedHashMap<String, Map<String, Object>> byCat = new LinkedHashMap<>();
                for (Map<String, Object> r : raw) {
                    Map<String, Object> row = byCat.computeIfAbsent(String.valueOf(r.get("code")), k -> {
                        Map<String, Object> n = new LinkedHashMap<>();
                        n.put("category", r.get("category"));
                        for (String mk : keys) n.put(mk, BigDecimal.ZERO);
                        n.put("total", BigDecimal.ZERO);
                        return n;
                    });
                    int idx = ((Number) r.get("idx")).intValue();
                    BigDecimal amt = (BigDecimal) r.get("amount");
                    if (idx >= 0 && idx < 12) row.put("m" + idx, ((BigDecimal) row.get("m" + idx)).add(amt));
                    row.put("total", ((BigDecimal) row.get("total")).add(amt));
                }
                rows = new ArrayList<>(byCat.values());
                rows.sort((a, b) -> ((BigDecimal) b.get("total")).compareTo((BigDecimal) a.get("total")));
            }
            default -> throw new BusinessValidationException("Unknown Report", "FAMILY_REPORT_UNKNOWN", "Report '" + key + "' does not exist.", "Pick a report from the list.");
        }
        return new Report(key, title, period, cols, rows, totals(cols, rows));
    }

    @Transactional(readOnly = true)
    public byte[] export(String key, String format, LocalDate from, LocalDate to, String category, String mode) {
        Report r = report(key, from, to, category, mode);
        String[] headers = r.columns().stream().map(c -> String.valueOf(c.get("label"))).toArray(String[]::new);
        List<Object[]> rows = new ArrayList<>();
        for (Map<String, Object> row : r.rows()) {
            Object[] line = new Object[headers.length];
            for (int i = 0; i < headers.length; i++) {
                Map<String, Object> c = r.columns().get(i);
                Object v = row.get(String.valueOf(c.get("key")));
                if (v instanceof java.sql.Date d) v = d.toLocalDate();
                if (v instanceof LocalDate d) v = d.format(D);
                line[i] = v;
            }
            rows.add(line);
        }
        if (!r.rows().isEmpty()) {
            Object[] total = new Object[headers.length];
            total[0] = "Total";
            for (int i = 1; i < headers.length; i++) total[i] = r.totals().get(String.valueOf(r.columns().get(i).get("key")));
            rows.add(total);
        }
        return exports.render(format, r.title() + " (" + r.period() + ")", headers, rows);
    }

    // =====================================================================================================  helpers

    private static List<Map<String, Object>> cols(String... spec) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String s : spec) {
            String[] p = s.split(":", 3);
            out.add(new LinkedHashMap<>(Map.of("key", p[0], "label", p[1], "type", p[2])));
        }
        return out;
    }

    private static Map<String, Object> totals(List<Map<String, Object>> cols, List<Map<String, Object>> rows) {
        Map<String, Object> t = new LinkedHashMap<>();
        for (Map<String, Object> c : cols) {
            String type = String.valueOf(c.get("type"));
            if (!type.equals("money") && !type.equals("number")) continue;
            BigDecimal sum = BigDecimal.ZERO;
            for (Map<String, Object> r : rows) {
                Object v = r.get(String.valueOf(c.get("key")));
                if (v instanceof Number n) sum = sum.add(new BigDecimal(n.toString()));
            }
            t.put(String.valueOf(c.get("key")), sum);
        }
        return t;
    }

    /** Default period: this month. A reversed range is swapped; at most 10 years. */
    private static LocalDate[] period(LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : YearMonth.now().atDay(1);
        LocalDate t = to != null ? to : LocalDate.now();
        if (t.isBefore(f)) { LocalDate x = f; f = t; t = x; }
        if (f.isBefore(t.minusYears(10))) f = t.minusYears(10);
        return new LocalDate[]{f, t};
    }

    private static MapSqlParameterSource params(Long cid, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource("cid", cid).addValue("from", from).addValue("to", to);
    }

    private static String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static String str(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }
}
