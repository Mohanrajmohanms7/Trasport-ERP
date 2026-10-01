package com.transport.erp.service;

import com.transport.erp.dto.OpeningBalanceRequest;
import com.transport.erp.dto.SparePartRequest;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.LookupValueRepository;
import com.transport.erp.repository.UomMasterRepository;
import com.transport.erp.security.TenantAccessService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel bulk creation for masters and opening stock (the data a company loads once when it starts, or adds in batches).
 * Transactions (bookings, trips, invoices…) are deliberately NOT bulk-created: they need approvals and postings.
 *
 * Flow: download template → upload → every row validated (required, formats, dropdown values, duplicates in the file and
 * in the database, business rules) → preview with row errors → remove invalid rows → create. Create re-validates on the
 * server and creates all rows in ONE transaction through the normal services; any failure rolls back everything.
 */
@Service
public class BulkImportService {

    public static final int MAX_ROWS = 1000;
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy")};

    @Autowired private TenantAccessService tenantAccess;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private LookupValueRepository lookupRepository;
    @Autowired private UomMasterRepository uomRepository;
    @Autowired private CustomerService customerService;
    @Autowired private VehicleService vehicleService;
    @Autowired private DriverService driverService;
    @Autowired private SupplierService supplierService;
    @Autowired private MaterialService materialService;
    @Autowired private SparePartService sparePartService;
    @Autowired private InventoryService inventoryService;

    /** kind: text | code | number | int | date | email | phone | gstin | enum:A,B | lookup:TYPE | branch | warehouse | part | uom */
    public record Column(String key, String header, boolean required, String kind, String example, String help) { }

    public record Module(String key, String title, List<Column> columns) { }

    public record RowResult(int rowNumber, Map<String, String> values, List<String> errors) {
        public boolean isValid() { return errors.isEmpty(); }
    }

    public record ValidationResult(String module, List<Column> columns, List<RowResult> rows, int total, int valid, int invalid) { }

    private static Column c(String key, String header, boolean req, String kind, String ex, String help) {
        return new Column(key, header, req, kind, ex, help);
    }

    private static final Map<String, Module> MODULES = new LinkedHashMap<>();

    static {
        MODULES.put("customers", new Module("customers", "Customers", List.of(
                c("code", "Customer Code", true, "code", "PKC-C11", "Unique in your company"),
                c("name", "Customer Name", true, "text", "Sri Murugan Constructions", ""),
                c("gstNumber", "GSTIN", false, "gstin", "33AAGFS1234K1Z5", "15 characters; decides CGST+SGST or IGST"),
                c("phone", "Phone", false, "phone", "9443100001", "10-digit mobile"),
                c("email", "Email", false, "email", "accounts@example.com", ""),
                c("address", "Billing Address", false, "text", "Trichy Main Road, Perambalur", ""),
                c("creditLimit", "Credit Limit (₹)", false, "number", "500000", "0 or more"),
                c("status", "Status", false, "enum:ACTIVE,INACTIVE", "ACTIVE", "Blank = ACTIVE"))));
        MODULES.put("vehicles", new Module("vehicles", "Vehicles", List.of(
                c("code", "Registration No", true, "code", "TN46AB1234", "Unique; letters and digits"),
                c("name", "Display Name", true, "text", "TN46AB1234 — Tata Tipper", ""),
                c("type", "Vehicle Type", false, "lookup:VEHICLE_TYPE", "", "From Admin → Dropdown Lists"),
                c("category", "Category", false, "lookup:VEHICLE_CATEGORY", "", "From Admin → Dropdown Lists"),
                c("capacity", "Capacity", false, "lookup:VEHICLE_CAPACITY", "", "From Admin → Dropdown Lists"),
                c("brand", "Brand", false, "text", "Tata", ""),
                c("model", "Model", false, "text", "Signa 2823.K", ""),
                c("chassisNumber", "Chassis No", false, "text", "MAT445000", ""),
                c("engineNumber", "Engine No", false, "text", "B6880000", ""),
                c("ownerType", "Ownership", false, "enum:SELF,HIRED,CLIENT", "SELF", "SELF = own, HIRED = attached, CLIENT = client owned"),
                c("ownerName", "Owner Name", false, "text", "PKC Transport", ""),
                c("branch", "Branch Code", false, "branch", "HO", "Blank = your branch / head office"),
                c("purchaseDate", "Purchase Date", false, "date", "2024-04-15", "YYYY-MM-DD or DD-MM-YYYY"),
                c("insuranceExpiryDate", "Insurance Valid Till", false, "date", "2027-03-31", ""),
                c("fitnessExpiryDate", "Fitness Valid Till", false, "date", "2027-06-30", ""),
                c("permitExpiryDate", "Permit Valid Till", false, "date", "2027-01-31", ""),
                c("status", "Status", false, "enum:ACTIVE,INACTIVE", "ACTIVE", "Blank = ACTIVE"))));
        MODULES.put("drivers", new Module("drivers", "Drivers", List.of(
                c("code", "Driver Code", true, "code", "PKC-D07", "Unique in your company"),
                c("name", "Driver Name", true, "text", "K. Murugan", ""),
                c("licenseNumber", "Licence No", true, "text", "TN46 20110004512", "Unique in your company"),
                c("licenseExpiryDate", "Licence Valid Till", false, "date", "2029-05-31", ""),
                c("phoneNumber", "Phone", false, "phone", "9500100001", "10-digit mobile"),
                c("branch", "Branch Code", false, "branch", "HO", "Blank = your branch / head office"),
                c("status", "Status", false, "enum:ACTIVE,INACTIVE", "ACTIVE", "Blank = ACTIVE"))));
        MODULES.put("suppliers", new Module("suppliers", "Suppliers", List.of(
                c("code", "Supplier Code", true, "code", "PKC-S11", "Unique in your company"),
                c("name", "Supplier Name", true, "text", "Perambalur Auto Spares", ""),
                c("gstNumber", "GSTIN", false, "gstin", "33AAPFP1111A1Z1", ""),
                c("phone", "Phone", false, "phone", "9443200001", ""),
                c("email", "Email", false, "email", "", ""),
                c("address", "Address", false, "text", "Perambalur", ""),
                c("creditDays", "Credit Days", false, "int", "30", "0 – 365"),
                c("status", "Status", false, "enum:ACTIVE,INACTIVE", "ACTIVE", "Blank = ACTIVE"))));
        MODULES.put("materials", new Module("materials", "Materials", List.of(
                c("code", "Material Code", true, "code", "MSAND", "Unique in your company"),
                c("name", "Material Name", true, "text", "M-Sand", ""),
                c("unit", "Default order unit (UOM code)", false, "uom", "UNIT", "Must exist in UOM master; used on orders when switched on"),
                c("defaultRate", "Default Rate (₹)", false, "number", "950", "0 or more"),
                c("status", "Status", false, "enum:ACTIVE,INACTIVE", "ACTIVE", "Blank = ACTIVE"))));
        MODULES.put("spare-parts", new Module("spare-parts", "Spare Parts", List.of(
                c("code", "Part Code", true, "code", "OF-01", "Unique in your company"),
                c("name", "Part Name", true, "text", "Engine Oil Filter", ""),
                c("unit", "Unit (UOM code)", true, "uom", "NOS", "Must exist in UOM master"),
                c("defaultRate", "Default Rate (₹)", false, "number", "450", "0 or more"),
                c("reorderLevel", "Reorder Level", false, "number", "6", "Low-stock alert level"),
                c("status", "Status", false, "enum:ACTIVE,INACTIVE", "ACTIVE", "Blank = ACTIVE"))));
        MODULES.put("opening-stock", new Module("opening-stock", "Opening Stock", List.of(
                c("warehouse", "Warehouse Code", true, "warehouse", "WH-PBR", "Must exist"),
                c("part", "Part Code", true, "part", "OF-01", "Must exist in Spare Parts"),
                c("quantity", "Quantity", true, "number", "10", "More than 0"),
                c("unitRate", "Unit Cost (₹)", false, "number", "450", "Values the stock in accounts"),
                c("description", "Remarks", false, "text", "Stock at go-live", ""))));
    }

    public List<Module> modules() {
        return new ArrayList<>(MODULES.values());
    }

    private Module module(String key) {
        Module m = MODULES.get(key);
        if (m == null) {
            throw new BusinessValidationException("Upload Not Available", "BULK_MODULE_UNKNOWN",
                    "Excel upload is not available for '" + key + "'.", "Use one of: " + String.join(", ", MODULES.keySet()));
        }
        return m;
    }

    // ------------------------------------------------------------------ template

    public byte[] template(String key) {
        Module m = module(key);
        Long companyId = tenantAccess.resolveCompanyId(null);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(m.title());
            Sheet lists = wb.createSheet("Lists");
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            bold.setColor(IndexedColors.WHITE.getIndex());
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row h = sheet.createRow(0);
            Row ex = sheet.createRow(1);
            Row note = sheet.createRow(2);
            int listCol = 0;
            for (int i = 0; i < m.columns().size(); i++) {
                Column col = m.columns().get(i);
                Cell cell = h.createCell(i);
                cell.setCellValue(col.header() + (col.required() ? " *" : ""));
                cell.setCellStyle(head);
                ex.createCell(i).setCellValue(col.example());
                sheet.setColumnWidth(i, Math.max(14, col.header().length() + 6) * 256);
                List<String> options = optionsFor(col, companyId);
                if (!options.isEmpty()) {
                    for (int r = 0; r < options.size(); r++) {
                        Row lr = lists.getRow(r) != null ? lists.getRow(r) : lists.createRow(r);
                        lr.createCell(listCol).setCellValue(options.get(r));
                    }
                    String letter = org.apache.poi.ss.util.CellReference.convertNumToColString(listCol);
                    DataValidationHelper dv = sheet.getDataValidationHelper();
                    DataValidationConstraint cons = dv.createFormulaListConstraint("Lists!$" + letter + "$1:$" + letter + "$" + options.size());
                    DataValidation v = dv.createValidation(cons, new CellRangeAddressList(1, MAX_ROWS, i, i));
                    v.setShowErrorBox(true);
                    sheet.addValidationData(v);
                    listCol++;
                }
            }
            note.createCell(0).setCellValue("Row 2 is an example — replace it with your data. Columns with * are required. Keep the header row.");
            wb.setSheetHidden(1, true);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build the template: " + e.getMessage(), e);
        }
    }

    private List<String> optionsFor(Column col, Long companyId) {
        String k = col.kind();
        if (k.startsWith("enum:")) return Arrays.asList(k.substring(5).split(","));
        if (k.startsWith("lookup:")) {
            return lookupRepository.findByCompanyIdAndTypeAndIsDeletedFalse(companyId, k.substring(7)).stream()
                    .map(LookupValue::getName).filter(Objects::nonNull).distinct().limit(200).toList();
        }
        String sql = switch (k) {
            case "branch" -> "SELECT code FROM branches WHERE company_id = ? AND is_deleted = false ORDER BY code";
            case "warehouse" -> "SELECT code FROM warehouses WHERE company_id = ? AND is_deleted = false ORDER BY code";
            case "part" -> "SELECT code FROM spare_parts WHERE company_id = ? AND is_deleted = false ORDER BY code";
            case "uom" -> "SELECT DISTINCT code FROM uom_master WHERE (company_id = ? OR company_id IS NULL) AND is_deleted = false ORDER BY code";
            default -> null;
        };
        return sql == null ? List.of() : jdbc.queryForList(sql + " LIMIT 500", String.class, companyId);
    }

    // ------------------------------------------------------------------ parse + validate

    public ValidationResult validateFile(String key, MultipartFile file) {
        Module m = module(key);
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("No File", "BULK_FILE_REQUIRED", "Choose an Excel file (.xlsx).", "Download the template, fill it and upload.");
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new BusinessValidationException("Wrong File Type", "BULK_FILE_TYPE", "Upload an Excel file (.xlsx).", "Use the downloaded template.");
        }
        List<Map<String, String>> rows = new ArrayList<>();
        List<Integer> rowNumbers = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter(Locale.ENGLISH);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new IllegalArgumentException("The sheet is empty.");
            Map<Integer, Column> byIndex = new HashMap<>();
            for (Cell cell : header) {
                String hv = norm(fmt.formatCellValue(cell));
                for (Column col : m.columns()) {
                    if (norm(col.header()).equals(hv)) byIndex.put(cell.getColumnIndex(), col);
                }
            }
            List<String> missing = m.columns().stream().filter(Column::required)
                    .filter(col -> !byIndex.containsValue(col)).map(Column::header).toList();
            if (!missing.isEmpty()) {
                throw new BusinessValidationException("Wrong Template", "BULK_HEADERS_MISSING",
                        "Required column(s) missing: " + String.join(", ", missing) + ".", "Download the template for " + m.title() + " and use its header row.");
            }
            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                boolean any = false;
                for (Map.Entry<Integer, Column> e : byIndex.entrySet()) {
                    Cell cell = row.getCell(e.getKey());
                    String v = cellText(cell, fmt);
                    values.put(e.getValue().key(), v);
                    if (!v.isBlank()) any = true;
                }
                if (!any) continue;
                if (values.values().stream().anyMatch(v -> v.startsWith("Row 2 is an example"))) continue;
                rows.add(values);
                rowNumbers.add(r + 1);
                if (rows.size() > MAX_ROWS) {
                    throw new BusinessValidationException("Too Many Rows", "BULK_TOO_MANY_ROWS",
                            "At most " + MAX_ROWS + " rows per upload.", "Split the file.");
                }
            }
        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessValidationException("Unreadable File", "BULK_FILE_UNREADABLE", "The Excel file could not be read: " + e.getMessage(), "Save it as .xlsx and try again.");
        }
        if (rows.isEmpty()) {
            throw new BusinessValidationException("No Rows", "BULK_NO_ROWS", "The file has no data rows.", "Fill at least one row below the header.");
        }
        return validateRows(key, rows, rowNumbers);
    }

    private static String cellText(Cell cell, DataFormatter fmt) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            BigDecimal b = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            return b.scale() < 0 ? b.setScale(0).toPlainString() : b.toPlainString();
        }
        return fmt.formatCellValue(cell).trim();
    }

    private static String norm(String s) {
        return s == null ? "" : s.replace("*", "").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Validates rows (from the file or sent back from the preview). */
    public ValidationResult validateRows(String key, List<Map<String, String>> rows, List<Integer> rowNumbers) {
        Module m = module(key);
        Long companyId = tenantAccess.resolveCompanyId(null);
        Map<String, Integer> seen = new HashMap<>();
        List<RowResult> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> v = new LinkedHashMap<>();
            for (Column col : m.columns()) v.put(col.key(), trim(rows.get(i).get(col.key())));
            int rn = rowNumbers != null && i < rowNumbers.size() ? rowNumbers.get(i) : i + 2;
            List<String> errs = new ArrayList<>();
            for (Column col : m.columns()) checkCell(col, v.get(col.key()), companyId, errs);
            businessChecks(key, v, companyId, errs);
            for (String dupKey : uniqueKeys(key, v)) {
                Integer first = seen.putIfAbsent(dupKey, rn);
                if (first != null) errs.add("Duplicate of row " + first + " in this file (" + dupKey.substring(dupKey.indexOf(':') + 1) + ").");
            }
            out.add(new RowResult(rn, v, errs));
        }
        int valid = (int) out.stream().filter(RowResult::isValid).count();
        return new ValidationResult(key, m.columns(), out, out.size(), valid, out.size() - valid);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void checkCell(Column col, String v, Long companyId, List<String> errs) {
        String label = col.header();
        if (v.isBlank()) {
            if (col.required()) errs.add(label + " is required.");
            return;
        }
        String k = col.kind();
        switch (k.contains(":") ? k.substring(0, k.indexOf(':')) : k) {
            case "code" -> {
                if (v.length() > 50) errs.add(label + " must be 50 characters or fewer.");
                else if (!v.matches("[A-Za-z0-9][A-Za-z0-9 ./_-]*")) errs.add(label + " may contain only letters, digits, space, - / _ .");
            }
            case "text" -> { if (v.length() > 250) errs.add(label + " is too long (max 250)."); }
            case "number" -> {
                BigDecimal b = num(v);
                if (b == null) errs.add(label + " must be a number (got '" + v + "').");
                else if (b.signum() < 0) errs.add(label + " cannot be negative.");
            }
            case "int" -> {
                if (!v.matches("\\d+")) errs.add(label + " must be a whole number (got '" + v + "').");
                else if (Long.parseLong(v) > 365) errs.add(label + " must be between 0 and 365.");
            }
            case "date" -> { if (date(v) == null) errs.add(label + " must be a date like 2026-04-15 or 15-04-2026 (got '" + v + "')."); }
            case "email" -> { if (!v.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) errs.add(label + " is not a valid email."); }
            case "phone" -> { if (!v.replaceAll("[\\s+-]", "").matches("(91)?\\d{10}")) errs.add(label + " must be a 10-digit number."); }
            case "gstin" -> {
                if (!v.toUpperCase(Locale.ROOT).matches("\\d{2}[A-Z0-9]{13}")) errs.add(label + " must be 15 characters starting with the 2-digit state code.");
            }
            case "enum" -> {
                List<String> allowed = Arrays.asList(k.substring(5).split(","));
                if (!allowed.contains(v.toUpperCase(Locale.ROOT))) errs.add(label + " must be one of " + String.join(", ", allowed) + " (got '" + v + "').");
            }
            case "lookup" -> { if (lookupId(companyId, k.substring(7), v) == null) errs.add(label + " '" + v + "' is not in Admin → Dropdown Lists."); }
            case "branch" -> { if (idByCode("branches", companyId, v) == null) errs.add(label + " '" + v + "' is not a branch of your company."); }
            case "warehouse" -> { if (idByCode("warehouses", companyId, v) == null) errs.add(label + " '" + v + "' is not an existing warehouse."); }
            case "part" -> { if (idByCode("spare_parts", companyId, v) == null) errs.add(label + " '" + v + "' is not an existing spare part."); }
            case "uom" -> { if (uom(companyId, v) == null) errs.add(label + " '" + v + "' is not in the UOM master."); }
            default -> { }
        }
    }

    /** Duplicates against existing data and other business rules. */
    private void businessChecks(String key, Map<String, String> v, Long companyId, List<String> errs) {
        String code = v.getOrDefault("code", "");
        switch (key) {
            case "customers" -> { if (!code.isBlank() && idByCode("customers", companyId, code) != null) errs.add("Customer code " + code + " already exists."); }
            case "vehicles" -> { if (!code.isBlank() && idByCode("vehicles", companyId, code) != null) errs.add("Vehicle " + code + " already exists."); }
            case "suppliers" -> { if (!code.isBlank() && idByCode("suppliers", companyId, code) != null) errs.add("Supplier code " + code + " already exists."); }
            case "materials" -> { if (!code.isBlank() && idByCode("materials", companyId, code) != null) errs.add("Material code " + code + " already exists."); }
            case "spare-parts" -> { if (!code.isBlank() && idByCode("spare_parts", companyId, code) != null) errs.add("Part code " + code + " already exists."); }
            case "drivers" -> {
                if (!code.isBlank() && idByCode("drivers", companyId, code) != null) errs.add("Driver code " + code + " already exists.");
                String lic = v.getOrDefault("licenseNumber", "");
                if (!lic.isBlank() && count("SELECT COUNT(*) FROM drivers WHERE company_id = ? AND is_deleted = false AND lower(license_number) = lower(?)", companyId, lic) > 0) {
                    errs.add("Licence " + lic + " already belongs to another driver.");
                }
                LocalDate exp = date(v.getOrDefault("licenseExpiryDate", ""));
                if (exp != null && exp.isBefore(LocalDate.now().minusYears(5))) errs.add("Licence expiry date looks wrong (" + exp + ").");
            }
            case "opening-stock" -> {
                Long wh = idByCode("warehouses", companyId, v.getOrDefault("warehouse", ""));
                Long part = idByCode("spare_parts", companyId, v.getOrDefault("part", ""));
                BigDecimal q = num(v.getOrDefault("quantity", ""));
                if (q != null && q.signum() == 0) errs.add("Quantity must be more than 0.");
                if (wh != null && part != null && count("SELECT COUNT(*) FROM inventory_transactions WHERE company_id = ? AND is_deleted = false"
                        + " AND warehouse_id = ? AND spare_part_id = ?", companyId, wh, part) > 0) {
                    errs.add("This part already has stock movements in this warehouse — use Receive stock instead of opening stock.");
                }
            }
            default -> { }
        }
        if (key.equals("vehicles")) {
            for (String d : List.of("insuranceExpiryDate", "fitnessExpiryDate", "permitExpiryDate")) {
                LocalDate x = date(v.getOrDefault(d, ""));
                if (x != null && x.getYear() < 2000) errs.add("Date " + x + " looks wrong.");
            }
            LocalDate p = date(v.getOrDefault("purchaseDate", ""));
            if (p != null && p.isAfter(LocalDate.now())) errs.add("Purchase date cannot be in the future.");
        }
    }

    private static List<String> uniqueKeys(String key, Map<String, String> v) {
        List<String> keys = new ArrayList<>();
        String code = v.getOrDefault("code", "").toLowerCase(Locale.ROOT);
        if (!code.isBlank()) keys.add("code:" + v.get("code"));
        if (key.equals("drivers") && !v.getOrDefault("licenseNumber", "").isBlank()) keys.add("licence:" + v.get("licenseNumber"));
        if (key.equals("opening-stock")) keys.add("stock:" + v.get("warehouse") + " / " + v.get("part"));
        return keys.stream().map(s -> s.toLowerCase(Locale.ROOT).startsWith("code:") ? "code:" + s.substring(5).toLowerCase(Locale.ROOT) : s.toLowerCase(Locale.ROOT)).toList();
    }

    // ------------------------------------------------------------------ create (all or nothing)

    @Transactional
    public Map<String, Object> create(String key, List<Map<String, String>> rows, String username) {
        if (rows == null || rows.isEmpty()) {
            throw new BusinessValidationException("Nothing To Create", "BULK_NO_ROWS", "There are no rows to create.", "Upload a file first.");
        }
        ValidationResult check = validateRows(key, rows, null);
        if (check.invalid() > 0) {
            RowResult bad = check.rows().stream().filter(r -> !r.isValid()).findFirst().orElseThrow();
            throw new BusinessValidationException("Invalid Rows", "BULK_INVALID_ROWS",
                    check.invalid() + " row(s) are not valid (first: " + String.join(" ", bad.errors()) + ")",
                    "Click Remove invalid, then Create.");
        }
        Long companyId = tenantAccess.resolveCompanyId(null);
        int n = 0;
        for (RowResult r : check.rows()) {
            n++;
            try {
                createOne(key, r.values(), companyId, username);
            } catch (RuntimeException e) {
                // Rolls back every row created so far — no partial uploads.
                throw new BusinessValidationException("Row " + n + " Failed", "BULK_ROW_FAILED",
                        "Row " + n + " (" + firstNonBlank(r.values().get("code"), r.values().get("part"), "") + "): " + e.getMessage()
                                + " Nothing was created.", "Correct that row and upload again.");
            }
        }
        return Map.of("module", key, "created", n);
    }

    private static String firstNonBlank(String... s) {
        for (String x : s) if (x != null && !x.isBlank()) return x;
        return "";
    }

    private void createOne(String key, Map<String, String> v, Long companyId, String username) {
        String status = v.getOrDefault("status", "").isBlank() ? "ACTIVE" : v.get("status").toUpperCase(Locale.ROOT);
        switch (key) {
            case "customers" -> {
                Customer c = new Customer();
                c.setCode(v.get("code"));
                c.setName(v.get("name"));
                c.setGstNumber(blankToNull(v.get("gstNumber")) == null ? null : v.get("gstNumber").toUpperCase(Locale.ROOT));
                c.setPhone(blankToNull(v.get("phone")));
                c.setEmail(blankToNull(v.get("email")));
                c.setAddress(blankToNull(v.get("address")));
                c.setCreditLimit(num(v.get("creditLimit")) == null ? BigDecimal.ZERO : num(v.get("creditLimit")));
                c.setStatus(status);
                c.setCompanyId(companyId);
                c.setCreatedBy(username);
                customerService.create(c);
            }
            case "vehicles" -> {
                Vehicle x = new Vehicle();
                x.setCode(v.get("code").toUpperCase(Locale.ROOT).replace(" ", ""));
                x.setName(v.get("name"));
                x.setType(lookup(companyId, "VEHICLE_TYPE", v.get("type")));
                x.setCategory(lookup(companyId, "VEHICLE_CATEGORY", v.get("category")));
                x.setCapacity(lookup(companyId, "VEHICLE_CAPACITY", v.get("capacity")));
                x.setBrand(blankToNull(v.get("brand")));
                x.setModel(blankToNull(v.get("model")));
                x.setChassisNumber(blankToNull(v.get("chassisNumber")));
                x.setEngineNumber(blankToNull(v.get("engineNumber")));
                x.setOwnerType(v.getOrDefault("ownerType", "").isBlank() ? "SELF" : v.get("ownerType").toUpperCase(Locale.ROOT));
                x.setOwnerName(blankToNull(v.get("ownerName")));
                x.setBranchId(idByCode("branches", companyId, v.get("branch")));
                x.setPurchaseDate(date(v.get("purchaseDate")));
                x.setInsuranceExpiryDate(date(v.get("insuranceExpiryDate")));
                x.setFitnessExpiryDate(date(v.get("fitnessExpiryDate")));
                x.setPermitExpiryDate(date(v.get("permitExpiryDate")));
                x.setStatus(status);
                x.setCompanyId(companyId);
                x.setCreatedBy(username);
                vehicleService.create(x);
            }
            case "drivers" -> {
                Driver d = new Driver();
                d.setCode(v.get("code"));
                d.setName(v.get("name"));
                d.setLicenseNumber(v.get("licenseNumber"));
                d.setLicenseExpiryDate(date(v.get("licenseExpiryDate")));
                d.setPhoneNumber(blankToNull(v.get("phoneNumber")));
                d.setBranchId(idByCode("branches", companyId, v.get("branch")));
                d.setStatus(status);
                d.setCompanyId(companyId);
                d.setCreatedBy(username);
                driverService.create(d);
            }
            case "suppliers" -> {
                Supplier s = new Supplier();
                s.setCode(v.get("code"));
                s.setName(v.get("name"));
                s.setGstNumber(blankToNull(v.get("gstNumber")));
                s.setPhone(blankToNull(v.get("phone")));
                s.setEmail(blankToNull(v.get("email")));
                s.setAddress(blankToNull(v.get("address")));
                s.setCreditDays(v.getOrDefault("creditDays", "").isBlank() ? null : Integer.valueOf(v.get("creditDays")));
                s.setStatus(status);
                s.setCompanyId(companyId);
                s.setCreatedBy(username);
                supplierService.create(s);
            }
            case "materials" -> {
                Material mt = new Material();
                mt.setCode(v.get("code"));
                mt.setName(v.get("name"));
                mt.setDefaultUom(uom(companyId, v.get("unit")));
                mt.setDefaultRate(num(v.get("defaultRate")));
                mt.setStatus(status);
                mt.setCompanyId(companyId);
                mt.setCreatedBy(username);
                materialService.create(mt);
            }
            case "spare-parts" -> {
                SparePartRequest p = new SparePartRequest();
                p.setCode(v.get("code"));
                p.setName(v.get("name"));
                UomMaster u = uom(companyId, v.get("unit"));
                p.setDefaultUomId(u == null ? null : u.getId());
                p.setDefaultRate(num(v.get("defaultRate")));
                p.setReorderLevel(num(v.get("reorderLevel")));
                p.setStatus(status);
                sparePartService.create(p, username);
            }
            case "opening-stock" -> {
                OpeningBalanceRequest o = new OpeningBalanceRequest();
                o.setWarehouseId(idByCode("warehouses", companyId, v.get("warehouse")));
                o.setSparePartId(idByCode("spare_parts", companyId, v.get("part")));
                o.setQuantity(num(v.get("quantity")));
                o.setUnitRate(num(v.get("unitRate")));
                o.setDescription(blankToNull(v.get("description")));
                inventoryService.createOpeningBalance(o, username);
            }
            default -> throw new IllegalArgumentException("Unknown module " + key);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static BigDecimal num(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replace(",", "").replace("₹", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate date(String s) {
        if (s == null || s.isBlank()) return null;
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(s.trim(), f);
            } catch (Exception ignored) { }
        }
        return null;
    }

    private Long idByCode(String table, Long companyId, String code) {
        if (code == null || code.isBlank()) return null;
        List<Long> ids = jdbc.queryForList("SELECT id FROM " + table + " WHERE company_id = ? AND is_deleted = false AND lower(code) = lower(?) ORDER BY id LIMIT 1",
                Long.class, companyId, code.trim());
        return ids.isEmpty() ? null : ids.get(0);
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private Long lookupId(Long companyId, String type, String value) {
        LookupValue l = lookup(companyId, type, value);
        return l == null ? null : l.getId();
    }

    /** Dropdown value by code or name (case-insensitive), same list the screens use. */
    private LookupValue lookup(Long companyId, String type, String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        return lookupRepository.findByCompanyIdAndTypeAndIsDeletedFalse(companyId, type).stream()
                .filter(l -> v.equalsIgnoreCase(l.getCode()) || v.equalsIgnoreCase(l.getName()))
                .findFirst().orElse(null);
    }

    private UomMaster uom(Long companyId, String code) {
        if (code == null || code.isBlank()) return null;
        List<Long> ids = jdbc.queryForList("SELECT id FROM uom_master WHERE (company_id = ? OR company_id IS NULL) AND is_deleted = false"
                + " AND lower(code) = lower(?) ORDER BY company_id NULLS LAST, id LIMIT 1", Long.class, companyId, code.trim());
        return ids.isEmpty() ? null : uomRepository.findById(ids.get(0)).orElse(null);
    }
}
