package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.model.FuelRequest;
import com.transport.erp.model.Trip;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Driver App (self-service for a DRIVER login). Every answer is limited to the driver linked to the logged-in user
 * (drivers.app_user_id) inside that user's company — a driver never sees another driver's trips, bata or requests.
 * Read-only except fuel requests, which are only allowed for the driver's own open trip.
 */
@Service
public class DriverAppService {

    @Autowired private AppUserRepository userRepository;
    @Autowired private DriverVehicleAuthorizationService driverAuthorization;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private TripRepository tripRepository;
    @Autowired private FuelRequestService fuelRequestService;

    /** The logged-in user and the driver record linked to it (refused when no driver is linked). */
    private record Me(AppUser user, Driver driver) {}

    private Me me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessValidationException("Not Signed In", "DRIVER_APP_NO_USER",
                        "Your login was not found.", "Sign in again."));
        return new Me(user, driverAuthorization.requireLinkedDriver(user));
    }

    private MapSqlParameterSource p(Me m) {
        return new MapSqlParameterSource("did", m.driver().getId()).addValue("cid", m.user().getCompanyId());
    }

    /** Own profile: name, code, phone, licence, regular lorry. */
    @Transactional(readOnly = true)
    public Map<String, Object> profile() {
        Me m = me();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("driverId", m.driver().getId());
        out.put("name", m.driver().getName());
        out.put("code", m.driver().getCode());
        out.put("phone", m.driver().getPhoneNumber());
        out.put("licenseNumber", m.driver().getLicenseNumber());
        out.put("licenseExpiryDate", m.driver().getLicenseExpiryDate());
        out.put("vehicles", driverAuthorization.authorizedVehicles(m.user()));
        return out;
    }

    /** Own trips between two dates (default: last 30 days to today), newest first, with their material lines. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> trips(LocalDate from, LocalDate to) {
        Me m = me();
        LocalDate t = to != null ? to : LocalDate.now();
        LocalDate f = from != null ? from : t.minusDays(30);
        if (f.isAfter(t)) throw new BusinessValidationException("Invalid Dates", "DRIVER_APP_DATES", "'From' is after 'To'.", "Choose a valid date range.");
        if (f.plusDays(366).isBefore(t)) throw new BusinessValidationException("Range Too Long", "DRIVER_APP_RANGE", "Choose at most one year.", "Shorten the date range.");
        List<Map<String, Object>> trips = jdbc.queryForList("""
                SELECT t.id, t.trip_number AS "tripNumber", t.trip_date AS "tripDate", t.status,
                       t.customer_paid_bata AS "customerPaidBata", v.code AS "vehicle", b.booking_number AS "bookingNumber",
                       c.name AS "customer", ds.site_name AS "site", t.remarks
                  FROM trips t
                  LEFT JOIN vehicles v ON v.id = t.vehicle_id
                  LEFT JOIN bookings b ON b.id = t.booking_id
                  LEFT JOIN customers c ON c.id = b.customer_id
                  LEFT JOIN customer_delivery_sites ds ON ds.id = b.delivery_site_id
                 WHERE t.driver_id = :did AND t.company_id = :cid AND t.is_deleted = false
                   AND t.trip_date BETWEEN :f AND :t
                 ORDER BY t.trip_date DESC, t.id DESC
                 LIMIT 300""", p(m).addValue("f", f).addValue("t", t));
        if (trips.isEmpty()) return trips;
        List<Long> ids = trips.stream().map(r -> ((Number) r.get("id")).longValue()).toList();
        Map<Long, List<Map<String, Object>>> lines = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT td.trip_id, mt.name AS "material", td.quantity, td.loaded_quantity AS "loadedQuantity",
                       td.delivered_quantity AS "deliveredQuantity", COALESCE(u.symbol, u.code) AS "unit"
                  FROM trip_details td
                  LEFT JOIN materials mt ON mt.id = td.material_id
                  LEFT JOIN uom_master u ON u.id = td.uom_id
                 WHERE td.trip_id IN (:ids) AND td.is_deleted = false ORDER BY td.id""", new MapSqlParameterSource("ids", ids))) {
            Long tid = ((Number) r.remove("trip_id")).longValue();
            lines.computeIfAbsent(tid, k -> new ArrayList<>()).add(r);
        }
        for (Map<String, Object> r : trips) r.put("lines", lines.getOrDefault(((Number) r.get("id")).longValue(), List.of()));
        return trips;
    }

    /** Own bata for a month: bata paid by the owner (approved / paid expenses) and bata paid by customers (per trip). */
    @Transactional(readOnly = true)
    public Map<String, Object> bata(YearMonth month) {
        Me m = me();
        YearMonth ym = month != null ? month : YearMonth.now();
        MapSqlParameterSource ps = p(m).addValue("f", ym.atDay(1)).addValue("t", ym.atEndOfMonth());
        List<Map<String, Object>> owner = jdbc.queryForList("""
                SELECT e.expense_date AS "date", e.amount, t.trip_number AS "tripNumber", e.description
                  FROM expenses e LEFT JOIN trips t ON t.id = e.trip_id
                 WHERE e.driver_id = :did AND e.company_id = :cid AND e.is_deleted = false AND e.category = 'DRIVER_BATA'
                   AND e.status IN ('APPROVED', 'PAID') AND e.expense_date BETWEEN :f AND :t
                 ORDER BY e.expense_date, e.id""", ps);
        List<Map<String, Object>> customer = jdbc.queryForList("""
                SELECT t.trip_date AS "date", t.customer_paid_bata AS amount, t.trip_number AS "tripNumber", c.name AS "customer"
                  FROM trips t LEFT JOIN bookings b ON b.id = t.booking_id LEFT JOIN customers c ON c.id = b.customer_id
                 WHERE t.driver_id = :did AND t.company_id = :cid AND t.is_deleted = false AND t.status <> 'CANCELLED'
                   AND t.customer_paid_bata > 0 AND t.trip_date BETWEEN :f AND :t
                 ORDER BY t.trip_date, t.id""", ps);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", ym.toString());
        out.put("ownerPaid", owner);
        out.put("ownerPaidTotal", sum(owner));
        out.put("customerPaid", customer);
        out.put("customerPaidTotal", sum(customer));
        return out;
    }

    /** Own fuel requests (latest first). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> fuelRequests() {
        Me m = me();
        return jdbc.queryForList("""
                SELECT fr.id, fr.request_number AS "requestNumber", fr.status, fr.requested_quantity AS "requestedQuantity",
                       fr.requested_amount AS "requestedAmount", fr.created_date AS "createdDate", t.trip_number AS "tripNumber", v.code AS "vehicle"
                  FROM fuel_requests fr JOIN trips t ON t.id = fr.trip_id LEFT JOIN vehicles v ON v.id = t.vehicle_id
                 WHERE t.driver_id = :did AND t.company_id = :cid AND fr.is_deleted = false
                 ORDER BY fr.id DESC LIMIT 100""", p(m));
    }

    /** Fuel request for the driver's OWN open trip (planned / dispatched) — the usual approval flow follows. */
    @Transactional
    public FuelRequest requestFuel(Long tripId, BigDecimal litres, BigDecimal amount) {
        Me m = me();
        Trip trip = tripRepository.findById(tripId == null ? -1L : tripId)
                .filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException("Trip Not Found", "DRIVER_APP_TRIP", "Choose one of your trips.", "Pick the trip from the list."));
        if (trip.getDriver() == null || !m.driver().getId().equals(trip.getDriver().getId()) || !m.user().getCompanyId().equals(trip.getCompanyId())) {
            throw new BusinessValidationException("Not Your Trip", "DRIVER_APP_NOT_OWN_TRIP", "You can request fuel only for your own trips.", "Pick one of your trips.");
        }
        if (!"PLANNED".equals(trip.getStatus()) && !"DISPATCHED".equals(trip.getStatus())) {
            throw new BusinessValidationException("Trip Not Open", "DRIVER_APP_TRIP_CLOSED", "Fuel can be requested only for a planned or dispatched trip.", "Pick an open trip.");
        }
        if (litres == null || litres.signum() <= 0) {
            throw new BusinessValidationException("Invalid Quantity", "DRIVER_APP_FUEL_QTY", "Enter the litres needed.", "Enter a quantity above 0.");
        }
        FuelRequest req = new FuelRequest();
        req.setTrip(trip);
        req.setRequestedQuantity(litres);
        req.setRequestedAmount(amount == null ? BigDecimal.ZERO : amount);
        return fuelRequestService.createRequest(req, m.user().getUsername());
    }

    private static BigDecimal sum(List<Map<String, Object>> rows) {
        BigDecimal s = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) if (r.get("amount") instanceof BigDecimal b) s = s.add(b);
        return s;
    }
}
