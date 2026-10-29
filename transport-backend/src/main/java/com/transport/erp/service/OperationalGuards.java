package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Business rules that protect operational data (flow review, October 2026):
 * <ol>
 *   <li>a vehicle or driver is on at most one DISPATCHED trip at a time;</li>
 *   <li>fuel / expenses linked to a trip use that trip's vehicle and driver;</li>
 *   <li>inactive customers, vehicles and drivers are not used on new bookings / trips;</li>
 *   <li>one GSTIN per customer within a company;</li>
 *   <li>a supplier's bill number is entered only once (unless cancelled);</li>
 *   <li>a completed trip's date cannot move in or out of a month whose driver payroll is approved / paid.</li>
 * </ol>
 * Plain reads only; existing records stay editable where the rule says so.
 */
@Service
@RequiredArgsConstructor
public class OperationalGuards {

    private final NamedParameterJdbcTemplate jdbc;

    private static BusinessValidationException err(String title, String code, String message, String action) {
        return new BusinessValidationException(title, code, message, action);
    }

    // 1 ------------------------------------------------------------------
    /** Before dispatch: the vehicle and the driver must not already be out on another trip. */
    public void assertFreeToDispatch(Long tripId, Long companyId, Long vehicleId, Long driverId) {
        List<Map<String, Object>> busy = jdbc.queryForList("""
                SELECT t.trip_number, t.vehicle_id, t.driver_id FROM trips t
                 WHERE t.company_id = :cid AND t.is_deleted = false AND t.status = 'DISPATCHED' AND t.id <> :id
                   AND (t.vehicle_id = :vid OR t.driver_id = :did)
                 ORDER BY t.id LIMIT 1""",
                new MapSqlParameterSource("cid", companyId).addValue("id", tripId).addValue("vid", vehicleId).addValue("did", driverId));
        if (busy.isEmpty()) return;
        Map<String, Object> b = busy.get(0);
        boolean sameVehicle = Objects.equals(((Number) b.get("vehicle_id")).longValue(), vehicleId);
        String who = sameVehicle ? "This vehicle" : "This driver";
        throw err(sameVehicle ? "Vehicle On Another Trip" : "Driver On Another Trip", sameVehicle ? "TRIP_VEHICLE_BUSY" : "TRIP_DRIVER_BUSY",
                who + " is already out on trip " + b.get("trip_number") + ".",
                "Complete trip " + b.get("trip_number") + " first, or choose another " + (sameVehicle ? "vehicle." : "driver."));
    }

    // 2 ------------------------------------------------------------------
    /**
     * A fuel entry / expense linked to a trip must use the trip's vehicle and driver.
     * Returns the trip's [vehicleId, driverId] so the caller can fill blanks.
     */
    public Long[] assertMatchesTrip(Long tripId, Long companyId, Long vehicleId, Long driverId, String what) {
        if (tripId == null) return null;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT trip_number, vehicle_id, driver_id, company_id FROM trips WHERE id = :id AND is_deleted = false",
                new MapSqlParameterSource("id", tripId));
        if (rows.isEmpty()) throw err("Trip Not Found", "LINKED_TRIP_NOT_FOUND", "The trip linked to this " + what + " was not found.", "Choose the trip again.");
        Map<String, Object> t = rows.get(0);
        if (!Objects.equals(((Number) t.get("company_id")).longValue(), companyId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to another company's trip");
        }
        Long tv = t.get("vehicle_id") == null ? null : ((Number) t.get("vehicle_id")).longValue();
        Long td = t.get("driver_id") == null ? null : ((Number) t.get("driver_id")).longValue();
        if (vehicleId != null && tv != null && !vehicleId.equals(tv)) {
            throw err("Vehicle Does Not Match Trip", "LINKED_TRIP_VEHICLE_MISMATCH",
                    "Trip " + t.get("trip_number") + " is on a different vehicle than this " + what + ".",
                    "Choose the trip's vehicle, or link the " + what + " to a trip of this vehicle.");
        }
        if (driverId != null && td != null && !driverId.equals(td)) {
            throw err("Driver Does Not Match Trip", "LINKED_TRIP_DRIVER_MISMATCH",
                    "Trip " + t.get("trip_number") + " has a different driver than this " + what + ".",
                    "Choose the trip's driver, or link the " + what + " to that driver's trip.");
        }
        return new Long[]{tv, td};
    }

    // 3 ------------------------------------------------------------------
    /** New bookings / trips: the customer, vehicle or driver must be active. */
    public void assertActive(String table, Long id, String what) {
        if (id == null) return;
        List<String> st = jdbc.queryForList("SELECT COALESCE(status, 'ACTIVE') FROM " + table + " WHERE id = :id AND is_deleted = false",
                new MapSqlParameterSource("id", id), String.class);
        if (!st.isEmpty() && "INACTIVE".equalsIgnoreCase(st.get(0))) {
            throw err(what + " Inactive", "INACTIVE_" + what.toUpperCase().replace(' ', '_'),
                    "This " + what.toLowerCase() + " is inactive and cannot be used on new entries.",
                    "Activate it in its master first, or choose another " + what.toLowerCase() + ".");
        }
    }

    // 4 ------------------------------------------------------------------
    public void assertUniqueCustomerGstin(Long companyId, String gstin, Long excludeId) {
        if (gstin == null || gstin.isBlank()) return;
        List<String> other = jdbc.queryForList("""
                SELECT name FROM customers WHERE company_id = :cid AND is_deleted = false
                   AND UPPER(TRIM(gst_number)) = UPPER(TRIM(:g)) AND (:ex IS NULL OR id <> :ex) LIMIT 1""",
                new MapSqlParameterSource("cid", companyId).addValue("g", gstin).addValue("ex", excludeId, java.sql.Types.BIGINT), String.class);
        if (!other.isEmpty()) {
            throw err("GSTIN Already Used", "CUSTOMER_GSTIN_DUPLICATE",
                    "GSTIN " + gstin.trim().toUpperCase() + " is already on customer " + other.get(0) + ".",
                    "Use that customer (add another delivery site to it) instead of creating a second one.");
        }
    }

    // 5 ------------------------------------------------------------------
    public void assertUniqueSupplierBillNo(Long companyId, Long supplierId, String billNo, Long excludeId) {
        if (billNo == null || billNo.isBlank() || supplierId == null) return;
        List<String> other = jdbc.queryForList("""
                SELECT bill_number FROM supplier_bills WHERE company_id = :cid AND supplier_id = :sid AND is_deleted = false
                   AND COALESCE(status, '') <> 'CANCELLED' AND UPPER(TRIM(supplier_bill_no)) = UPPER(TRIM(:no))
                   AND (:ex IS NULL OR id <> :ex) LIMIT 1""",
                new MapSqlParameterSource("cid", companyId).addValue("sid", supplierId).addValue("no", billNo).addValue("ex", excludeId, java.sql.Types.BIGINT), String.class);
        if (!other.isEmpty()) {
            throw err("Bill Already Entered", "SUPPLIER_BILL_DUPLICATE",
                    "This supplier's bill " + billNo.trim() + " is already entered as " + other.get(0) + ".",
                    "Open that bill instead — paying it twice would pay the supplier twice.");
        }
    }

    // 6 ------------------------------------------------------------------
    /** A completed trip's date may not move in or out of a month whose payroll for this driver is approved / paid. */
    public void assertPayrollMonthOpen(Long driverId, LocalDate oldDate, LocalDate newDate) {
        if (driverId == null || oldDate == null || newDate == null || oldDate.equals(newDate)) return;
        for (LocalDate d : new LocalDate[]{oldDate, newDate}) {
            List<String> st = jdbc.queryForList("""
                    SELECT status FROM driver_payrolls WHERE driver_id = :did AND :d BETWEEN period_from AND period_to
                       AND COALESCE(is_deleted, false) = false AND status NOT IN ('DRAFT', 'CANCELLED') LIMIT 1""",
                    new MapSqlParameterSource("did", driverId).addValue("d", d, java.sql.Types.DATE), String.class);
            if (!st.isEmpty()) {
                throw err("Payroll Already Approved", "TRIP_DATE_PAYROLL_LOCKED",
                        "The driver's payroll covering " + d + " is " + st.get(0) + ", so this completed trip's date cannot change.",
                        "Keep the date, or cancel that payroll first and generate it again.");
            }
        }
    }

    /** The vehicle / driver must exist in this company (rejects another company's or a deleted id; reveals nothing about it). */
    public void assertBelongsToCompany(String table, Long id, Long companyId, String what) {
        if (id == null || companyId == null) return;
        List<Long> owner = jdbc.queryForList("SELECT company_id FROM " + table + " WHERE id = :id AND is_deleted = false",
                new MapSqlParameterSource("id", id), Long.class);
        if (owner.isEmpty() || !companyId.equals(owner.get(0))) {
            throw err(what + " Not Found", what.toUpperCase() + "_NOT_FOUND",
                    "The selected " + what.toLowerCase() + " was not found.", "Choose the " + what.toLowerCase() + " again from the list.");
        }
    }

    /** Title the fuel screen recognises to ask an admin "odometer replaced / reset?" (then resubmits with odometerReset). */
    public static final String ODOMETER_RESET_TITLE = "Odometer Lower Than Last Reading";

    /**
     * Fuel odometer rules. (1) Current below previous is always refused. (2) When {@code checkHistory}: previous below the
     * lorry's highest reading on or before this fuel date is refused unless an admin confirms a meter replacement / reset.
     */
    public void assertFuelOdometer(Long companyId, Long vehicleId, Long excludeId, java.time.LocalDate fuelDate,
                                   java.math.BigDecimal previous, java.math.BigDecimal current, Boolean reset, boolean checkHistory) {
        if (previous == null || current == null) return;
        if (current.compareTo(previous) < 0) {
            throw err("Odometer Going Backwards", "FUEL_ODOMETER_BACKWARDS",
                    "Current odometer (" + current.stripTrailingZeros().toPlainString() + ") is lower than previous ("
                            + previous.stripTrailingZeros().toPlainString() + ").",
                    "Check the reading on the meter and correct the current or previous odometer.");
        }
        if (!checkHistory || vehicleId == null || companyId == null) return;
        List<java.math.BigDecimal> last = jdbc.queryForList("""
                SELECT MAX(current_odometer) FROM fuel_entries
                 WHERE company_id = :cid AND vehicle_id = :vid AND is_deleted = false AND COALESCE(status, '') <> 'CANCELLED'
                   AND (:ex IS NULL OR id <> :ex) AND (CAST(:d AS date) IS NULL OR fuel_date <= CAST(:d AS date))""",
                new MapSqlParameterSource("cid", companyId).addValue("vid", vehicleId)
                        .addValue("ex", excludeId, java.sql.Types.BIGINT).addValue("d", fuelDate, java.sql.Types.DATE), java.math.BigDecimal.class);
        java.math.BigDecimal lastKm = last.isEmpty() ? null : last.get(0);
        if (lastKm == null || previous.compareTo(lastKm) >= 0) return;
        if (Boolean.TRUE.equals(reset)) {
            org.springframework.security.core.Authentication a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            boolean admin = a != null && a.getAuthorities().stream().anyMatch(g -> java.util.Set.of(
                    "ROLE_SUPER_ADMIN", "ROLE_COMPANY_ADMIN", "ROLE_ADMIN", "ROLE_BRANCH_MANAGER", "ROLE_FLEET_MANAGER").contains(g.getAuthority()));
            if (admin) return;
            throw err("Admin Confirmation Needed", "FUEL_ODOMETER_RESET_ADMIN",
                    "Only an admin can confirm that this lorry's odometer was replaced or reset.", "Ask your admin to save this fuel entry.");
        }
        throw err(ODOMETER_RESET_TITLE, "FUEL_ODOMETER_BELOW_LAST",
                "Previous odometer (" + previous.stripTrailingZeros().toPlainString() + ") is lower than this lorry's last reading ("
                        + lastKm.stripTrailingZeros().toPlainString() + "). Was the odometer replaced or reset?",
                "Correct the previous odometer, or (admin) confirm that the meter was replaced / reset.");
    }
}
