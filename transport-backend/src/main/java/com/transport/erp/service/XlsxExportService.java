package com.transport.erp.service;

import com.transport.erp.dto.CustomerLedgerPrintDTO;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class XlsxExportService {

    @Autowired private TenantAccessService tenantAccess;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private FuelEntryRepository fuelEntryRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private DriverPayrollRepository driverPayrollRepository;
    @Autowired private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired private CustomerReceiptRepository customerReceiptRepository;
    @Autowired private CustomerLedgerService customerLedgerService;
    @Autowired private ReportTemplateService reportTemplateService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private MaintenanceRequestRepository maintenanceRequestRepository;
    @Autowired private SparePartRepository sparePartRepository;
    @Autowired private WarehouseStockRepository warehouseStockRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private DriverAdvanceRepository driverAdvanceRepository;
    @Autowired private JournalVoucherRepository journalVoucherRepository;
    @Autowired private ChartOfAccountRepository chartOfAccountRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private WarehouseRepository warehouseRepository;

    // =========================================================================
    // EXPORT METHODS
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] exportMaterials(Long companyId) {
        return exportMaterials(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportMaterials(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<Material> list = materialRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Code", "Name", "Category", "UOM", "Default Rate", "Density", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Material m : list) {
            String uom = m.getDefaultUom() != null ? m.getDefaultUom().getName()
                    : (m.getUnit() != null ? m.getUnit().getName() : "");
            String category = m.getCategory() != null ? m.getCategory().getName() : "";
            rows.add(new Object[]{
                    m.getCode(),
                    m.getName(),
                    category,
                    uom,
                    m.getDefaultRate(),
                    m.getDensity(),
                    m.getStatus()
            });
        }
        return render(format, "Materials", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportVehicles(Long companyId) {
        return exportVehicles(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportVehicles(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<Vehicle> list = vehicleRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Code", "Name", "Chassis No", "Engine No", "Model", "Brand", "Type", "Category", "Capacity", "Owner Name", "Owner Type", "Purchase Date", "Insurance Expiry", "Permit Expiry", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Vehicle v : list) {
            rows.add(new Object[]{
                    v.getCode(),
                    v.getName(),
                    v.getChassisNumber(),
                    v.getEngineNumber(),
                    v.getModel(),
                    v.getBrand(),
                    v.getType() != null ? v.getType().getName() : "",
                    v.getCategory() != null ? v.getCategory().getName() : "",
                    v.getCapacity() != null ? v.getCapacity().getName() : "",
                    v.getOwnerName(),
                    v.getOwnerType(),
                    v.getPurchaseDate(),
                    v.getInsuranceExpiryDate(),
                    v.getPermitExpiryDate(),
                    v.getStatus()
            });
        }
        return render(format, "Vehicles", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportDrivers(Long companyId) {
        return exportDrivers(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportDrivers(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<Driver> list = driverRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Code", "Name", "License Number", "License Expiry", "Phone Number", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Driver d : list) {
            rows.add(new Object[]{
                    d.getCode(),
                    d.getName(),
                    d.getLicenseNumber(),
                    d.getLicenseExpiryDate(),
                    d.getPhoneNumber(),
                    d.getStatus()
            });
        }
        return render(format, "Drivers", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomers(Long companyId) {
        return exportCustomers(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomers(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<Customer> list = customerRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Code", "Name", "Email", "Phone", "Address", "GST Number", "Credit Limit", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Customer c : list) {
            rows.add(new Object[]{
                    c.getCode(),
                    c.getName(),
                    c.getEmail(),
                    c.getPhone(),
                    c.getAddress(),
                    c.getGstNumber(),
                    c.getCreditLimit(),
                    c.getStatus()
            });
        }
        return render(format, "Customers", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportBookings(Long companyId) {
        return exportBookings(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportBookings(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<Booking> list = bookingRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Booking No", "Booking Date", "Customer", "Delivery Site", "Priority", "Remarks", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Booking b : list) {
            rows.add(new Object[]{
                    b.getBookingNumber(),
                    b.getBookingDate(),
                    b.getCustomer() != null ? b.getCustomer().getName() : "",
                    b.getDeliverySite() != null ? b.getDeliverySite().getSiteName() : "",
                    b.getPriority(),
                    b.getRemarks(),
                    b.getStatus()
            });
        }
        return render(format, "Bookings", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportTrips(Long companyId) {
        return exportTrips(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportTrips(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<Trip> list = tripRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Trip No", "Trip Date", "Booking No", "Vehicle", "Driver", "Remarks", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Trip t : list) {
            rows.add(new Object[]{
                    t.getTripNumber(),
                    t.getTripDate(),
                    t.getBooking() != null ? t.getBooking().getBookingNumber() : "",
                    t.getVehicle() != null ? t.getVehicle().getName() : "",
                    t.getDriver() != null ? t.getDriver().getName() : "",
                    t.getRemarks(),
                    t.getStatus()
            });
        }
        return render(format, "Trips", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportFuelEntries(Long companyId) {
        return exportFuelEntries(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportFuelEntries(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<FuelEntry> list = fuelEntryRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Entry No", "Fuel Date", "Vehicle", "Driver", "Station", "Quantity (Litres)", "Rate/Litre", "Total Amount", "Payment Method", "Invoice No", "Odometer", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (FuelEntry f : list) {
            rows.add(new Object[]{
                    f.getFuelEntryNumber(),
                    f.getFuelDate(),
                    f.getVehicle() != null ? f.getVehicle().getName() : "",
                    f.getDriver() != null ? f.getDriver().getName() : "",
                    f.getFuelStation(),
                    f.getFuelQuantity(),
                    f.getRatePerLitre(),
                    f.getTotalAmount(),
                    f.getPaymentMethod(),
                    f.getInvoiceNumber(),
                    f.getCurrentOdometer(),
                    f.getStatus()
            });
        }
        return render(format, "Fuel Entries", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportExpenses(Long companyId) {
        return exportExpenses(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportExpenses(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<Expense> list = expenseRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Expense No", "Expense Date", "Category", "Vehicle", "Driver", "Description", "Amount", "GST Amount", "Total Amount", "Payment Method", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Expense e : list) {
            rows.add(new Object[]{
                    e.getExpenseNumber(),
                    e.getExpenseDate(),
                    e.getCategory(),
                    e.getVehicle() != null ? e.getVehicle().getName() : "",
                    e.getDriver() != null ? e.getDriver().getName() : "",
                    e.getDescription(),
                    e.getAmount(),
                    e.getGstAmount(),
                    e.getTotalAmount(),
                    e.getPaymentMethod(),
                    e.getStatus()
            });
        }
        return render(format, "Expenses", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportDriverPayroll(Long companyId) {
        return exportDriverPayroll(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportDriverPayroll(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<DriverPayroll> list = driverPayrollRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Payroll No", "Year", "Month", "Driver", "Basic Salary", "Allowance", "Deductions", "Advance Adj", "Net Payable", "Payment Method", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (DriverPayroll p : list) {
            rows.add(new Object[]{
                    p.getPayrollNumber(),
                    p.getPayYear(),
                    p.getPayMonth(),
                    p.getDriver() != null ? p.getDriver().getName() : "",
                    p.getBasicSalary(),
                    p.getAllowanceAmount(),
                    p.getDeductionAmount(),
                    p.getAdvanceAdjustment(),
                    p.getNetSalaryPayable(),
                    p.getPaymentMethod(),
                    p.getStatus()
            });
        }
        return render(format, "Driver Payroll", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesInvoices(Long companyId) {
        return exportSalesInvoices(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesInvoices(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<SalesInvoice> list = salesInvoiceRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Invoice No", "Invoice Date", "Customer", "Payment Terms", "Subtotal", "Discount", "Net Amount", "Paid Amount", "Payment Status", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (SalesInvoice i : list) {
            rows.add(new Object[]{
                    i.getInvoiceNumber(),
                    i.getInvoiceDate(),
                    i.getCustomer() != null ? i.getCustomer().getName() : "",
                    i.getPaymentTerms(),
                    i.getSubtotal(),
                    i.getDiscount(),
                    i.getNetAmount(),
                    i.getPaidAmount(),
                    i.getPaymentStatus(),
                    i.getStatus()
            });
        }
        return render(format, "Sales Invoices", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomerReceipts(Long companyId) {
        return exportCustomerReceipts(companyId, "xlsx");
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomerReceipts(Long companyId, String format) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<CustomerReceipt> list = customerReceiptRepository
                .findByCompanyIdAndIsDeletedFalse(scopedCompanyId, PageRequest.of(0, 5000))
                .getContent();

        String[] headers = {"Receipt No", "Receipt Date", "Customer", "Booking No", "Amount Received", "Advance Amount", "Payment Method", "Ref No", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (CustomerReceipt r : list) {
            rows.add(new Object[]{
                    r.getReceiptNumber(),
                    r.getReceiptDate(),
                    r.getCustomer() != null ? r.getCustomer().getName() : "",
                    r.getBooking() != null ? r.getBooking().getBookingNumber() : "",
                    r.getAmountReceived(),
                    r.getAdvanceAmount(),
                    r.getPaymentMethod(),
                    r.getReferenceNumber(),
                    r.getStatus()
            });
        }
        return render(format, "Customer Receipts", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportCustomerLedger(Long customerId) {
        CustomerLedgerPrintDTO dto = customerLedgerService.getLedgerPrintData(customerId);

        String[] headers = {"Transaction Date", "Transaction Type", "Reference Number", "Remarks / Description", "Debit", "Credit", "Running Balance"};
        List<Object[]> rows = new ArrayList<>();

        if (dto.getItems() != null) {
            for (CustomerLedgerPrintDTO.CustomerLedgerItemDTO item : dto.getItems()) {
                rows.add(new Object[]{
                        item.getTransactionDate(),
                        item.getTransactionType(),
                        item.getReferenceNumber(),
                        item.getRemarks(),
                        item.getDebitAmount(),
                        item.getCreditAmount(),
                        item.getRunningBalance()
                });
            }
        }
        return buildExcelWorkbook("Customer Ledger", headers, rows);
    }

    @Transactional(readOnly = true)
    public Map<String, String> exportTemplateXlsx(Long templateId) {
        ReportTemplate template = reportTemplateService.getTemplateById(templateId);
        tenantAccess.assertCompanyAccess(template.getCompanyId());
        Long companyId = template.getCompanyId();

        String reportType = template.getReportType() != null ? template.getReportType().toUpperCase() : "REVENUE";
        byte[] xlsxBytes = switch (reportType) {
            case "EXPENSE" -> exportExpenses(companyId);
            case "FUEL" -> exportFuelEntries(companyId);
            case "TRIP" -> exportTrips(companyId);
            case "FLEET" -> exportVehicles(companyId);
            default -> exportSalesInvoices(companyId);
        };

        String safeName = (template.getTemplateName() != null ? template.getTemplateName() : "report")
                .replaceAll("[^a-zA-Z0-9-_ ]", "")
                .trim()
                .replace(' ', '_');

        Map<String, String> result = new HashMap<>();
        result.put("fileName", safeName + ".xlsx");
        result.put("mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        result.put("contentBase64", Base64.getEncoder().encodeToString(xlsxBytes));
        result.put("status", "GENERATED");
        return result;
    }

    // =========================================================================
    // POI HELPER METHODS
    // =========================================================================

    /** Same table as Excel or as a landscape PDF. */
    public byte[] render(String format, String title, String[] headers, List<Object[]> rows) {
        if ("pdf".equalsIgnoreCase(format)) {
            return com.transport.erp.util.TablePdfGenerator.render(title, companyName(), headers, rows);
        }
        return buildExcelWorkbook(title, headers, rows);
    }

    private static String stockStatus(WarehouseStock st) {
        BigDecimal a = st.getAvailableQuantity() == null ? BigDecimal.ZERO : st.getAvailableQuantity();
        BigDecimal r = st.getSparePart() != null ? st.getSparePart().getReorderLevel() : null;
        return a.signum() <= 0 ? "Out of stock" : (r != null && a.compareTo(r) <= 0 ? "Reorder" : "OK");
    }

    private String companyName() {
        try {
            Long cid = tenantAccess.resolveCompanyId(null);
            return companyRepository.findById(cid).map(Company::getName).orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    // ---------------------------------------------------------------- additional list exports

    @Transactional(readOnly = true)
    public byte[] exportWorkOrders(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"WO No", "Vehicle", "Type", "Priority", "Status", "Opened", "Completed", "Supplier", "Estimated", "Actual"};
        List<Object[]> rows = new ArrayList<>();
        for (WorkOrder w : workOrderRepository.findByCompanyIdAndIsDeletedFalseOrderByIdDesc(cid)) {
            rows.add(new Object[]{w.getWorkOrderNumber(), w.getVehicle() != null ? w.getVehicle().getName() : "",
                    w.getMaintenanceType(), w.getPriority(), w.getStatus(), w.getOpenedAt(), w.getCompletedAt(),
                    w.getSupplier() != null ? w.getSupplier().getName() : "", w.getEstimatedCost(), w.getActualCost()});
        }
        return render(format, "Work Orders", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportMaintenanceRequests(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Request No", "Vehicle", "Driver", "Priority", "Status", "Requested", "Odometer (km)", "Description"};
        List<Object[]> rows = new ArrayList<>();
        for (MaintenanceRequest m : maintenanceRequestRepository.findByCompanyIdAndIsDeletedFalseOrderByIdDesc(cid)) {
            rows.add(new Object[]{m.getRequestNumber(), m.getVehicle() != null ? m.getVehicle().getName() : "",
                    m.getDriver() != null ? m.getDriver().getName() : "", m.getPriority(), m.getStatus(), m.getRequestedAt(),
                    m.getReportedOdometerKm(), m.getDescription()});
        }
        return render(format, "Maintenance Requests", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportWarehouses(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Code", "Name", "Description", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Warehouse w : warehouseRepository.findByCompanyIdAndIsDeletedFalseOrderByCodeAsc(cid)) {
            rows.add(new Object[]{w.getCode(), w.getName(), w.getDescription(), w.getStatus()});
        }
        return render(format, "Warehouses", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportSpareParts(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Code", "Name", "UOM", "Default Rate", "Reorder Level", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (SparePart p : sparePartRepository.findActiveByCompany(cid, PageRequest.of(0, 5000)).getContent()) {
            rows.add(new Object[]{p.getCode(), p.getName(), p.getDefaultUom() != null ? p.getDefaultUom().getName() : "",
                    p.getDefaultRate(), p.getReorderLevel(), p.getStatus()});
        }
        return render(format, "Spare Parts", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportStock(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Warehouse", "Part Code", "Part", "Available Qty", "Reorder Level", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (WarehouseStock st : warehouseStockRepository.findByCompanyIdAndIsDeletedFalse(cid)) {
            rows.add(new Object[]{st.getWarehouse() != null ? st.getWarehouse().getName() : "",
                    st.getSparePart() != null ? st.getSparePart().getCode() : "",
                    st.getSparePart() != null ? st.getSparePart().getName() : "", st.getAvailableQuantity(),
                    st.getSparePart() != null ? st.getSparePart().getReorderLevel() : null, stockStatus(st)});
        }
        return render(format, "Stock", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportInventoryTransactions(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Date", "Warehouse", "Part", "Type", "Qty", "Unit Rate", "Reference"};
        List<Object[]> rows = new ArrayList<>();
        for (InventoryTransaction t : inventoryTransactionRepository.findByCompanyIdAndIsDeletedFalseOrderByIdDesc(cid)) {
            rows.add(new Object[]{t.getCreatedDate(), t.getWarehouse() != null ? t.getWarehouse().getName() : "",
                    t.getSparePart() != null ? t.getSparePart().getName() : "", t.getTransactionType(), t.getQuantity(),
                    t.getUnitRate(), t.getExternalReference()});
        }
        return render(format, "Inventory Transactions", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportDriverAdvances(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Advance No", "Driver", "Date", "Amount", "Recovered", "Outstanding", "Method", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (DriverAdvance a : driverAdvanceRepository.search(cid, null, null, null, PageRequest.of(0, 5000)).getContent()) {
            rows.add(new Object[]{a.getAdvanceNumber(), a.getDriver() != null ? a.getDriver().getName() : "", a.getAdvanceDate(),
                    a.getAmount(), a.getRecoveredAmount(), a.getOutstandingAmount(), a.getPaymentMethod(), a.getStatus()});
        }
        return render(format, "Driver Advances", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportJournal(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Voucher No", "Date", "Debit Account", "Credit Account", "Amount", "Reference", "Narration"};
        List<Object[]> rows = new ArrayList<>();
        for (JournalVoucher v : journalVoucherRepository.findByCompanyIdAndIsDeletedFalse(cid,
                PageRequest.of(0, 10000, org.springframework.data.domain.Sort.by("voucherDate").descending())).getContent()) {
            rows.add(new Object[]{v.getVoucherNumber(), v.getVoucherDate(),
                    v.getDebitAccount() != null ? v.getDebitAccount().getAccountCode() + " " + v.getDebitAccount().getAccountName() : "",
                    v.getCreditAccount() != null ? v.getCreditAccount().getAccountCode() + " " + v.getCreditAccount().getAccountName() : "",
                    v.getAmount(), v.getReferenceNumber(), v.getDescription()});
        }
        return render(format, "Journal (Day Book)", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportChartOfAccounts(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Code", "Account", "Type", "Opening Balance", "Current Balance"};
        List<Object[]> rows = new ArrayList<>();
        for (ChartOfAccount a : chartOfAccountRepository.findByCompanyIdAndIsDeletedFalseOrderByAccountCodeAsc(cid)) {
            rows.add(new Object[]{a.getAccountCode(), a.getAccountName(), a.getAccountType(), a.getOpeningBalance(), a.getRunningBalance()});
        }
        return render(format, "Chart of Accounts", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportSuppliers(Long companyId, String format) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String[] headers = {"Code", "Name", "Phone", "Email", "GSTIN", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (Supplier s : supplierRepository.findByCompanyIdAndIsDeletedFalse(cid, PageRequest.of(0, 5000)).getContent()) {
            rows.add(new Object[]{s.getCode(), s.getName(), s.getPhone(), s.getEmail(), s.getGstNumber(), s.getStatus()});
        }
        return render(format, "Suppliers", headers, rows);
    }

    public byte[] buildExcelWorkbook(String sheetName, String[] headers, List<Object[]> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName != null ? sheetName : "Sheet1");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Data Styles
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setBorderTop(BorderStyle.THIN);
            textStyle.setBorderBottom(BorderStyle.THIN);
            textStyle.setBorderLeft(BorderStyle.THIN);
            textStyle.setBorderRight(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));
            numberStyle.setBorderTop(BorderStyle.THIN);
            numberStyle.setBorderBottom(BorderStyle.THIN);
            numberStyle.setBorderLeft(BorderStyle.THIN);
            numberStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("yyyy-mm-dd"));
            dateStyle.setBorderTop(BorderStyle.THIN);
            dateStyle.setBorderBottom(BorderStyle.THIN);
            dateStyle.setBorderLeft(BorderStyle.THIN);
            dateStyle.setBorderRight(BorderStyle.THIN);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create Data Rows
            int rowIndex = 1;
            for (Object[] rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int colIndex = 0; colIndex < headers.length && colIndex < rowData.length; colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    Object val = rowData[colIndex];

                    if (val == null) {
                        cell.setCellValue("");
                        cell.setCellStyle(textStyle);
                    } else if (val instanceof BigDecimal bd) {
                        cell.setCellValue(bd.doubleValue());
                        cell.setCellStyle(numberStyle);
                    } else if (val instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                        cell.setCellStyle(numberStyle);
                    } else if (val instanceof LocalDate ld) {
                        cell.setCellValue(ld.toString());
                        cell.setCellStyle(dateStyle);
                    } else if (val instanceof LocalDateTime ldt) {
                        cell.setCellValue(ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                        cell.setCellStyle(dateStyle);
                    } else if (val instanceof Boolean b) {
                        cell.setCellValue(b ? "YES" : "NO");
                        cell.setCellStyle(textStyle);
                    } else {
                        cell.setCellValue(val.toString());
                        cell.setCellStyle(textStyle);
                    }
                }
            }

            // Auto-size columns with bounds check
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth < 3000) {
                    sheet.setColumnWidth(i, 3000);
                } else if (currentWidth > 12000) {
                    sheet.setColumnWidth(i, 12000);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate XLSX workbook", e);
        }
    }
}
