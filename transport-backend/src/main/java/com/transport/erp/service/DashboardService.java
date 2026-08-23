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
    @Autowired private TripRepository tripRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private FuelEntryRepository fuelEntryRepository;
    @Autowired private CustomerReceiptRepository customerReceiptRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboard() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.from(today).atDay(1);

        long todayTrips = tripRepository.countByCompanyIdAndTripDateAndIsDeletedFalse(companyId, today);
        long runningTrips = tripRepository.countByCompanyIdAndStatusInAndIsDeletedFalse(companyId, RUNNING_TRIP_STATUSES);
        long completedTrips = tripRepository.countByCompanyIdAndTripDateAndStatusAndIsDeletedFalse(companyId, today, "COMPLETED");
        long cancelledTrips = tripRepository.countByCompanyIdAndTripDateAndStatusAndIsDeletedFalse(companyId, today, "CANCELLED");

        long totalVehicles = vehicleRepository.countByCompanyIdAndIsDeletedFalse(companyId);
        long runningVehicles = tripRepository.countDistinctVehiclesOnTrips(companyId, RUNNING_TRIP_STATUSES);
        long availableVehicles = Math.max(0, totalVehicles - runningVehicles);
        long availableDrivers = driverRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "ACTIVE");

        BigDecimal revenueToday = nz(salesInvoiceRepository.sumNetAmountByCompanyAndDateRange(
                companyId, today, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal monthlyRevenue = nz(salesInvoiceRepository.sumNetAmountByCompanyAndDateRange(
                companyId, monthStart, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal fuelCost = nz(fuelEntryRepository.sumTotalAmountByCompanyAndDateRange(companyId, monthStart, today));
        BigDecimal todayExpenses = nz(expenseRepository.sumTotalAmountByCompanyAndDateRange(companyId, today, today));
        BigDecimal pendingPayments = nz(salesInvoiceRepository.sumNetAmountByCompanyAndStatuses(
                companyId, List.of("PENDING", "APPROVED")));
        BigDecimal totalInvoiced = nz(salesInvoiceRepository.sumNetAmountByCompanyAndStatuses(
                companyId, BILLABLE_INVOICE_STATUSES));
        BigDecimal totalCollected = nz(customerReceiptRepository.sumAmountReceivedByCompany(companyId));
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
        data.put("monthlyRevenueTrend", monthlyTrend(companyId, true));
        data.put("monthlyExpenseTrend", monthlyTrend(companyId, false));
        data.put("recentActivities", recentActivities(companyId));
        data.put("alerts", buildAlerts(companyId, today));
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOwnerDashboard() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.from(today).atDay(1);

        BigDecimal income = nz(salesInvoiceRepository.sumNetAmountByCompanyAndDateRange(
                companyId, monthStart, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal expense = nz(expenseRepository.sumTotalAmountByCompanyAndDateRange(companyId, monthStart, today))
                .add(nz(fuelEntryRepository.sumTotalAmountByCompanyAndDateRange(companyId, monthStart, today)));
        BigDecimal monthlyProfit = income.subtract(expense);

        long totalVehicles = vehicleRepository.countByCompanyIdAndIsDeletedFalse(companyId);
        long runningVehicles = tripRepository.countDistinctVehiclesOnTrips(companyId, RUNNING_TRIP_STATUSES);
        int utilization = totalVehicles == 0 ? 0
                : (int) Math.round((runningVehicles * 100.0) / totalVehicles);

        BigDecimal totalInvoiced = nz(salesInvoiceRepository.sumNetAmountByCompanyAndStatuses(
                companyId, BILLABLE_INVOICE_STATUSES));
        BigDecimal totalCollected = nz(customerReceiptRepository.sumAmountReceivedByCompany(companyId));
        BigDecimal outstandingAmount = totalInvoiced.subtract(totalCollected).max(BigDecimal.ZERO);
        BigDecimal fuelCost = nz(fuelEntryRepository.sumTotalAmountByCompanyAndDateRange(companyId, monthStart, today));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("monthlyProfit", monthlyProfit);
        data.put("income", income);
        data.put("expense", expense);
        data.put("vehicleUtilization", utilization);
        data.put("outstandingAmount", outstandingAmount);
        data.put("fuelCost", fuelCost);
        data.put("revenueTrend", monthlyTrend(companyId, true));
        data.put("profitTrend", profitTrend(companyId));
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOperationsDashboard() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        LocalDate today = LocalDate.now();

        long todayDispatch = tripRepository.countByCompanyIdAndTripDateAndStatusInAndIsDeletedFalse(
                companyId, today, RUNNING_TRIP_STATUSES);
        long tripsInProgress = tripRepository.countByCompanyIdAndStatusInAndIsDeletedFalse(
                companyId, RUNNING_TRIP_STATUSES);
        long tripsDelayed = tripRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "DELAYED");
        long pendingDispatch = tripRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "PLANNED")
                + bookingRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "APPROVED");
        long loadingQueueCount = tripRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "LOADING");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayDispatch", todayDispatch);
        data.put("tripsInProgress", tripsInProgress);
        data.put("tripsDelayed", tripsDelayed);
        data.put("pendingDispatch", pendingDispatch);
        data.put("loadingQueueCount", loadingQueueCount);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVehicleDashboard() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(30);

        long totalVehicles = vehicleRepository.countByCompanyIdAndIsDeletedFalse(companyId);
        long runningVehicles = tripRepository.countDistinctVehiclesOnTrips(companyId, RUNNING_TRIP_STATUSES);
        long availableVehicles = Math.max(0, totalVehicles - runningVehicles);
        long vehiclesInService = vehicleRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "INACTIVE")
                + vehicleRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "MAINTENANCE");
        long insuranceExpiryCount = vehicleRepository.countExpiringInsurance(companyId, today, soon);
        long permitExpiryCount = vehicleRepository.countExpiringPermit(companyId, today, soon);
        long maintenanceDueCount = vehicleRepository.countExpiringFitness(companyId, today, soon);

        int utilization = totalVehicles == 0 ? 0
                : (int) Math.round((runningVehicles * 100.0) / totalVehicles);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("availableVehicles", availableVehicles);
        data.put("vehiclesInService", vehiclesInService);
        data.put("insuranceExpiryCount", insuranceExpiryCount);
        data.put("permitExpiryCount", permitExpiryCount);
        data.put("maintenanceDueCount", maintenanceDueCount);
        data.put("vehicleUtilization", utilization);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAccountDashboard() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.from(today).atDay(1);

        BigDecimal collectionsToday = nz(customerReceiptRepository.sumAmountReceivedByCompanyAndDateRange(
                companyId, today, today));
        BigDecimal totalInvoiced = nz(salesInvoiceRepository.sumNetAmountByCompanyAndStatuses(
                companyId, BILLABLE_INVOICE_STATUSES));
        BigDecimal totalCollected = nz(customerReceiptRepository.sumAmountReceivedByCompany(companyId));
        BigDecimal outstandingTotal = totalInvoiced.subtract(totalCollected).max(BigDecimal.ZERO);
        BigDecimal pendingPayments = nz(salesInvoiceRepository.sumNetAmountByCompanyAndStatuses(
                companyId, List.of("PENDING", "APPROVED")));
        BigDecimal incomeThisMonth = nz(salesInvoiceRepository.sumNetAmountByCompanyAndDateRange(
                companyId, monthStart, today, BILLABLE_INVOICE_STATUSES));
        BigDecimal expenseThisMonth = nz(expenseRepository.sumTotalAmountByCompanyAndDateRange(companyId, monthStart, today))
                .add(nz(fuelEntryRepository.sumTotalAmountByCompanyAndDateRange(companyId, monthStart, today)));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("collectionsToday", collectionsToday);
        data.put("outstandingTotal", outstandingTotal);
        data.put("pendingPayments", pendingPayments);
        data.put("incomeThisMonth", incomeThisMonth);
        data.put("expenseThisMonth", expenseThisMonth);
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDriverDashboard() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        LocalDate today = LocalDate.now();

        long assignedTrips = tripRepository.countByCompanyIdAndStatusInAndIsDeletedFalse(
                companyId, RUNNING_TRIP_STATUSES);
        long completedTrips = tripRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "COMPLETED");
        long upcomingTrips = tripRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "PLANNED");
        long fuelEntries = fuelEntryRepository.countByCompanyIdAndFuelDateAndIsDeletedFalse(companyId, today);

        long closed = completedTrips + tripRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "CANCELLED");
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

    private List<BigDecimal> monthlyTrend(Long companyId, boolean revenue) {
        List<BigDecimal> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 4; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            if (revenue) {
                trend.add(nz(salesInvoiceRepository.sumNetAmountByCompanyAndDateRange(
                        companyId, start, end, BILLABLE_INVOICE_STATUSES)));
            } else {
                BigDecimal expenses = nz(expenseRepository.sumTotalAmountByCompanyAndDateRange(companyId, start, end));
                BigDecimal fuel = nz(fuelEntryRepository.sumTotalAmountByCompanyAndDateRange(companyId, start, end));
                trend.add(expenses.add(fuel));
            }
        }
        return trend;
    }

    private List<BigDecimal> profitTrend(Long companyId) {
        List<BigDecimal> revenue = monthlyTrend(companyId, true);
        List<BigDecimal> expense = monthlyTrend(companyId, false);
        List<BigDecimal> profit = new ArrayList<>();
        for (int i = 0; i < revenue.size(); i++) {
            profit.add(revenue.get(i).subtract(expense.get(i)));
        }
        return profit;
    }

    private List<Map<String, String>> buildAlerts(Long companyId, LocalDate today) {
        List<Map<String, String>> alerts = new ArrayList<>();
        LocalDate soon = today.plusDays(30);
        long insurance = vehicleRepository.countExpiringInsurance(companyId, today, soon);
        long permit = vehicleRepository.countExpiringPermit(companyId, today, soon);
        long pendingBookings = bookingRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, "PENDING");

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

    private List<Map<String, String>> recentActivities(Long companyId) {
        List<Map<String, String>> activities = new ArrayList<>();
        tripRepository.findTop5ByCompanyIdAndIsDeletedFalseOrderByIdDesc(companyId).forEach(t ->
                activities.add(Map.of(
                        "action", "Trip " + (t.getStatus() != null ? t.getStatus() : ""),
                        "details", "Trip " + nullSafe(t.getTripNumber()) + " on " + String.valueOf(t.getTripDate()),
                        "user", nullSafe(t.getUpdatedBy()),
                        "time", "recent")));
        bookingRepository.findTop5ByCompanyIdAndIsDeletedFalseOrderByIdDesc(companyId).forEach(b ->
                activities.add(Map.of(
                        "action", "Booking",
                        "details", "Booking " + nullSafe(b.getBookingNumber()) + " status " + nullSafe(b.getStatus()),
                        "user", nullSafe(b.getUpdatedBy()),
                        "time", "recent")));
        return activities.stream().limit(8).toList();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    private static String nullSafe(String value) {
        return value != null ? value : "-";
    }
}
