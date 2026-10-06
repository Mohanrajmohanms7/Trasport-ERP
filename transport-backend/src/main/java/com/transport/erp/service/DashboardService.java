package com.transport.erp.service;

import com.transport.erp.repository.BookingRepository;
import com.transport.erp.repository.CustomerReceiptRepository;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.ExpenseRepository;
import com.transport.erp.repository.FuelEntryRepository;
import com.transport.erp.repository.SalesInvoiceRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final List<String> RUNNING_TRIP_STATUSES = List.of(
            "DISPATCHED", "IN_TRANSIT", "LOADING", "UNLOADING", "ARRIVED", "ALLOCATED"
    );
    private static final List<String> BILLABLE_INVOICE_STATUSES = List.of(
            "PENDING", "APPROVED", "GENERATED", "PARTIAL", "UNPAID"
    );

    @Autowired private TenantAccessService tenantAccess;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    @Autowired private TripRepository tripRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private FuelEntryRepository fuelEntryRepository;
    @Autowired private CustomerReceiptRepository customerReceiptRepository;
    @Autowired private MaintenanceDueDashboardService maintenanceDueDashboardService;

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboard(Long branchId) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Scope sc = scope(companyId, branchId);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.from(today).atDay(1);

        long todayTrips = tripsOnDate(sc, today);
        long runningTrips = tripsInStatuses(sc, RUNNING_TRIP_STATUSES);
        long completedTrips = tripsOnDateWithStatus(sc, today, "COMPLETED");
        long cancelledTrips = tripsOnDateWithStatus(sc, today, "CANCELLED");

        long totalVehicles = vehicles(sc);
        long runningVehicles = vehiclesOnTrips(sc, RUNNING_TRIP_STATUSES);
        long availableVehicles = Math.max(0, totalVehicles - runningVehicles);
        long availableDrivers = driversWithStatus(sc, "ACTIVE");

        BigDecimal revenueToday = nz(invoicedBetween(sc, today, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal monthlyRevenue = nz(invoicedBetween(sc, monthStart, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal fuelCost = nz(fuelBetween(sc, monthStart, today));
        BigDecimal todayExpenses = nz(expensesBetween(sc, today, today));
        BigDecimal pendingPayments = nz(invoicedInStatuses(sc, List.of("PENDING", "APPROVED")));
        BigDecimal totalInvoiced = nz(invoicedInStatuses(sc, BILLABLE_INVOICE_STATUSES));
        BigDecimal totalCollected = nz(received(sc));
        BigDecimal outstandingAmount = totalInvoiced.subtract(totalCollected).max(BigDecimal.ZERO);

        int utilization = totalVehicles == 0 ? 0
                : (int) Math.round((runningVehicles * 100.0) / totalVehicles);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayTrips", todayTrips);
        data.put("runningTrips", runningTrips);
        data.put("completedTrips", completedTrips);
        data.put("cancelledTrips", cancelledTrips);
        data.put("availableVehicles", availableVehicles);
        data.put("runningVehicles", runningVehicles);
        data.put("availableDrivers", availableDrivers);
        data.put("revenueToday", revenueToday);
        data.put("monthlyRevenue", monthlyRevenue);
        data.put("fuelCost", fuelCost);
        data.put("todayExpenses", todayExpenses);
        data.put("pendingPayments", pendingPayments);
        data.put("outstandingAmount", outstandingAmount);
        data.put("vehicleUtilization", utilization);
        data.put("monthlyRevenueTrend", monthlyTrend(sc, true));
        data.put("monthlyExpenseTrend", monthlyTrend(sc, false));
        data.put("recentActivities", recentActivities(sc));
        data.put("alerts", buildAlerts(sc, today));
        data.put("maintenanceDueDashboard", maintenanceDueDashboardService.build(companyId, today));
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOwnerDashboard(Long branchId) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Scope sc = scope(companyId, branchId);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.from(today).atDay(1);

        BigDecimal income = nz(invoicedBetween(sc, monthStart, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal expense = nz(expensesBetween(sc, monthStart, today))
                .add(nz(fuelBetween(sc, monthStart, today)));
        BigDecimal monthlyProfit = income.subtract(expense);

        long totalVehicles = vehicles(sc);
        long runningVehicles = vehiclesOnTrips(sc, RUNNING_TRIP_STATUSES);
        int utilization = totalVehicles == 0 ? 0
                : (int) Math.round((runningVehicles * 100.0) / totalVehicles);

        BigDecimal totalInvoiced = nz(invoicedInStatuses(sc, BILLABLE_INVOICE_STATUSES));
        BigDecimal totalCollected = nz(received(sc));
        BigDecimal outstandingAmount = totalInvoiced.subtract(totalCollected).max(BigDecimal.ZERO);
        BigDecimal fuelCost = nz(fuelBetween(sc, monthStart, today));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monthlyProfit", monthlyProfit);
        data.put("income", income);
        data.put("expense", expense);
        data.put("vehicleUtilization", utilization);
        data.put("outstandingAmount", outstandingAmount);
        data.put("fuelCost", fuelCost);
        data.put("revenueTrend", monthlyTrend(sc, true));
        data.put("profitTrend", profitTrend(sc));
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOperationsDashboard(Long branchId) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Scope sc = scope(companyId, branchId);
        LocalDate today = LocalDate.now();

        long todayDispatch = tripsOnDateInStatuses(sc, today, RUNNING_TRIP_STATUSES);
        long tripsInProgress = tripsInStatuses(sc, RUNNING_TRIP_STATUSES);
        long tripsDelayed = tripsWithStatus(sc, "DELAYED");
        long pendingDispatch = tripsWithStatus(sc, "PLANNED")
                + bookingsWithStatus(sc, "APPROVED");
        long loadingQueueCount = tripsWithStatus(sc, "LOADING");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayDispatch", todayDispatch);
        data.put("tripsInProgress", tripsInProgress);
        data.put("tripsDelayed", tripsDelayed);
        data.put("pendingDispatch", pendingDispatch);
        data.put("loadingQueueCount", loadingQueueCount);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVehicleDashboard(Long branchId) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Scope sc = scope(companyId, branchId);
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(30);

        long totalVehicles = vehicles(sc);
        long runningVehicles = vehiclesOnTrips(sc, RUNNING_TRIP_STATUSES);
        long availableVehicles = Math.max(0, totalVehicles - runningVehicles);
        long vehiclesInService = vehiclesWithStatus(sc, "INACTIVE")
                + vehiclesWithStatus(sc, "MAINTENANCE");
        long insuranceExpiryCount = insuranceExpiring(sc, today, soon);
        long permitExpiryCount = permitExpiring(sc, today, soon);
        long maintenanceDueCount = fitnessExpiring(sc, today, soon);

        int utilization = totalVehicles == 0 ? 0
                : (int) Math.round((runningVehicles * 100.0) / totalVehicles);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("availableVehicles", availableVehicles);
        data.put("vehiclesInService", vehiclesInService);
        data.put("insuranceExpiryCount", insuranceExpiryCount);
        data.put("permitExpiryCount", permitExpiryCount);
        data.put("maintenanceDueCount", maintenanceDueCount);
        data.put("vehicleUtilization", utilization);
        data.put("maintenanceDueDashboard", maintenanceDueDashboardService.build(companyId, today));
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAccountDashboard(Long branchId) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Scope sc = scope(companyId, branchId);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.from(today).atDay(1);

        BigDecimal collectionsToday = nz(receivedBetween(sc, today, today));
        BigDecimal totalInvoiced = nz(invoicedInStatuses(sc, BILLABLE_INVOICE_STATUSES));
        BigDecimal totalCollected = nz(received(sc));
        BigDecimal outstandingTotal = totalInvoiced.subtract(totalCollected).max(BigDecimal.ZERO);
        BigDecimal pendingPayments = nz(invoicedInStatuses(sc, List.of("PENDING", "APPROVED")));
        BigDecimal incomeThisMonth = nz(invoicedBetween(sc, monthStart, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal expenseThisMonth = nz(expensesBetween(sc, monthStart, today))
                .add(nz(fuelBetween(sc, monthStart, today)));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("collectionsToday", collectionsToday);
        data.put("outstandingTotal", outstandingTotal);
        data.put("pendingPayments", pendingPayments);
        data.put("incomeThisMonth", incomeThisMonth);
        data.put("expenseThisMonth", expenseThisMonth);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDriverDashboard(Long branchId) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        Scope sc = scope(companyId, branchId);
        LocalDate today = LocalDate.now();

        long assignedTrips = tripsInStatuses(sc, RUNNING_TRIP_STATUSES);
        long completedTrips = tripsWithStatus(sc, "COMPLETED");
        long upcomingTrips = tripsWithStatus(sc, "PLANNED");
        long fuelEntries = fuelEntriesOn(sc, today);

        long closed = completedTrips + tripsWithStatus(sc, "CANCELLED");
        int attendancePercentage = closed == 0 ? 0
                : (int) Math.round((completedTrips * 100.0) / closed);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assignedTrips", assignedTrips);
        data.put("completedTrips", completedTrips);
        data.put("upcomingTrips", upcomingTrips);
        data.put("attendancePercentage", attendancePercentage);
        data.put("fuelEntries", fuelEntries);
        return data;
    }

    private List<BigDecimal> monthlyTrend(Scope sc, boolean revenue) {
        List<BigDecimal> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 4; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            if (revenue) {
                trend.add(nz(invoicedBetween(sc, start, end, BILLABLE_INVOICE_STATUSES)));
            } else {
                BigDecimal expenses = nz(expensesBetween(sc, start, end));
                BigDecimal fuel = nz(fuelBetween(sc, start, end));
                trend.add(expenses.add(fuel));
            }
        }
        return trend;
    }

    private List<BigDecimal> profitTrend(Scope sc) {
        List<BigDecimal> revenue = monthlyTrend(sc, true);
        List<BigDecimal> expense = monthlyTrend(sc, false);
        List<BigDecimal> profit = new ArrayList<>();
        for (int i = 0; i < revenue.size(); i++) {
            profit.add(revenue.get(i).subtract(expense.get(i)));
        }
        return profit;
    }

    private List<Map<String, String>> buildAlerts(Scope sc, LocalDate today) {
        List<Map<String, String>> alerts = new ArrayList<>();
        LocalDate soon = today.plusDays(30);
        long insurance = insuranceExpiring(sc, today, soon);
        long permit = permitExpiring(sc, today, soon);
        long pendingBookings = bookingsWithStatus(sc, "PENDING");

        if (insurance > 0) {
            alerts.add(Map.of(
                    "title", "Insurance Expiry Alert",
                    "message", insurance + " vehicle(s) have insurance expiring within 30 days.",
                    "type", "danger",
                    "time", "today"));
        }
        if (permit > 0) {
            alerts.add(Map.of(
                    "title", "Permit Renewal Due",
                    "message", permit + " vehicle(s) have permit expiring within 30 days.",
                    "type", "warning",
                    "time", "today"));
        }
        if (pendingBookings > 0) {
            alerts.add(Map.of(
                    "title", "Pending Booking Approval",
                    "message", pendingBookings + " booking(s) awaiting approval.",
                    "type", "info",
                    "time", "today"));
        }
        return alerts;
    }

    private List<Map<String, String>> recentActivities(Scope sc) {
        List<Map<String, String>> activities = new ArrayList<>();
        latestTrips(sc).forEach(t ->
                activities.add(Map.of(
                        "action", "Trip " + (t.getStatus() != null ? t.getStatus() : ""),
                        "details", "Trip " + nullSafe(t.getTripNumber()) + " on " + String.valueOf(t.getTripDate()),
                        "user", nullSafe(t.getUpdatedBy()),
                        "time", "recent")));
        latestBookings(sc).forEach(b ->
                activities.add(Map.of(
                        "action", "Booking",
                        "details", "Booking " + nullSafe(b.getBookingNumber()) + " status " + nullSafe(b.getStatus()),
                        "user", nullSafe(b.getUpdatedBy()),
                        "time", "recent")));
        return activities.stream().limit(8).toList();
    }

    // ================================================================ branch scope (docs/ACCESS_CONTROL.md)

    /** Company + branch the figures are for; branchId null = every branch of the company. */
    record Scope(Long companyId, Long branchId) { }

    /**
     * Branch users always see their own branch; company admins see the whole company or the one branch they pick
     * (checked to belong to their company). Same conditions as the old company-wide totals, plus the branch.
     */
    private Scope scope(Long companyId, Long requestedBranchId) {
        Long own = tenantAccess.listBranchScope();
        if (own != null) return new Scope(companyId, own);
        if (requestedBranchId == null) return new Scope(companyId, null);
        return new Scope(companyId, tenantAccess.resolveBranchId(requestedBranchId));
    }

    private <T> jakarta.persistence.TypedQuery<T> q(Scope sc, Class<T> type, String select, String entity, String cond, Object... kv) {
        jakarta.persistence.TypedQuery<T> query = em.createQuery(select + " FROM " + entity + " e WHERE e.companyId = :companyId"
                + " AND e.isDeleted = false AND (:branchId IS NULL OR e.branchId = :branchId)" + (cond.isEmpty() ? "" : " AND " + cond), type);
        query.setParameter("companyId", sc.companyId());
        query.setParameter("branchId", sc.branchId());
        for (int i = 0; i + 1 < kv.length; i += 2) query.setParameter((String) kv[i], kv[i + 1]);
        return query;
    }
    private long count(Scope sc, String entity, String cond, Object... kv) {
        Long n = q(sc, Long.class, "SELECT COUNT(e)", entity, cond, kv).getSingleResult();
        return n == null ? 0 : n;
    }
    private BigDecimal sum(Scope sc, String entity, String field, String cond, Object... kv) {
        return q(sc, BigDecimal.class, "SELECT COALESCE(SUM(e." + field + "), 0)", entity, cond, kv).getSingleResult();
    }

    private long tripsOnDate(Scope sc, LocalDate d) { return count(sc, "Trip", "e.tripDate = :d", "d", d); }
    private long tripsOnDateInStatuses(Scope sc, LocalDate d, java.util.Collection<String> st) { return count(sc, "Trip", "e.tripDate = :d AND e.status IN :st", "d", d, "st", st); }
    private long tripsOnDateWithStatus(Scope sc, LocalDate d, String st) { return count(sc, "Trip", "e.tripDate = :d AND e.status = :st", "d", d, "st", st); }
    private long tripsInStatuses(Scope sc, java.util.Collection<String> st) { return count(sc, "Trip", "e.status IN :st", "st", st); }
    private long tripsWithStatus(Scope sc, String st) { return count(sc, "Trip", "e.status = :st", "st", st); }
    private long vehiclesOnTrips(Scope sc, java.util.Collection<String> st) {
        Long n = q(sc, Long.class, "SELECT COUNT(DISTINCT e.vehicle.id)", "Trip", "e.status IN :st AND e.vehicle IS NOT NULL", "st", st).getSingleResult();
        return n == null ? 0 : n;
    }
    private long vehicles(Scope sc) { return count(sc, "Vehicle", ""); }
    private long vehiclesWithStatus(Scope sc, String st) { return count(sc, "Vehicle", "e.status = :st", "st", st); }
    private long insuranceExpiring(Scope sc, LocalDate a, LocalDate b) { return count(sc, "Vehicle", "e.insuranceExpiryDate IS NOT NULL AND e.insuranceExpiryDate BETWEEN :a AND :b", "a", a, "b", b); }
    private long permitExpiring(Scope sc, LocalDate a, LocalDate b) { return count(sc, "Vehicle", "e.permitExpiryDate IS NOT NULL AND e.permitExpiryDate BETWEEN :a AND :b", "a", a, "b", b); }
    private long fitnessExpiring(Scope sc, LocalDate a, LocalDate b) { return count(sc, "Vehicle", "e.fitnessExpiryDate IS NOT NULL AND e.fitnessExpiryDate BETWEEN :a AND :b", "a", a, "b", b); }
    private long driversWithStatus(Scope sc, String st) { return count(sc, "Driver", "e.status = :st", "st", st); }
    private long bookingsWithStatus(Scope sc, String st) { return count(sc, "Booking", "e.status = :st", "st", st); }
    private BigDecimal invoicedBetween(Scope sc, LocalDate a, LocalDate b, java.util.Collection<String> st) { return sum(sc, "SalesInvoice", "netAmount", "e.invoiceDate BETWEEN :a AND :b AND e.status IN :st", "a", a, "b", b, "st", st); }
    private BigDecimal invoicedInStatuses(Scope sc, java.util.Collection<String> st) { return sum(sc, "SalesInvoice", "netAmount", "e.status IN :st", "st", st); }
    private BigDecimal fuelBetween(Scope sc, LocalDate a, LocalDate b) { return sum(sc, "FuelEntry", "totalAmount", "e.fuelDate BETWEEN :a AND :b", "a", a, "b", b); }
    private long fuelEntriesOn(Scope sc, LocalDate d) { return count(sc, "FuelEntry", "e.fuelDate = :d", "d", d); }
    private BigDecimal expensesBetween(Scope sc, LocalDate a, LocalDate b) { return sum(sc, "Expense", "totalAmount", "e.expenseDate BETWEEN :a AND :b AND e.status <> 'CANCELLED'", "a", a, "b", b); }
    private BigDecimal received(Scope sc) { return sum(sc, "CustomerReceipt", "amountReceived", ""); }
    private BigDecimal receivedBetween(Scope sc, LocalDate a, LocalDate b) { return sum(sc, "CustomerReceipt", "amountReceived", "e.receiptDate BETWEEN :a AND :b", "a", a, "b", b); }
    private List<com.transport.erp.model.Trip> latestTrips(Scope sc) {
        return q(sc, com.transport.erp.model.Trip.class, "SELECT e", "Trip", "1 = 1 ORDER BY e.id DESC").setMaxResults(5).getResultList();
    }
    private List<com.transport.erp.model.Booking> latestBookings(Scope sc) {
        return q(sc, com.transport.erp.model.Booking.class, "SELECT e", "Booking", "1 = 1 ORDER BY e.id DESC").setMaxResults(5).getResultList();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    private static String nullSafe(String value) {
        return value != null ? value : "-";
    }
}
