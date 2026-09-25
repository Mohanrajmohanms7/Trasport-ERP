package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.DriverPaySlab;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Daily slab payroll maths. Pure logic, no database access.
 *
 * Trips are grouped per calendar day; each day earns the ONE slab its trip count falls in
 * (e.g. 1 trip = 500, 2+ trips = 1000). Trips are never multiplied individually.
 */
public final class DriverPayrollCalculator {

    private DriverPayrollCalculator() {
    }

    /** One computed day. */
    public record Day(LocalDate date, int tripCount, Integer slabFrom, Integer slabTo, BigDecimal amount) {
    }

    /** Month result. */
    public record Result(List<Day> days, int totalTrips, int tripDays, BigDecimal tripEarnings) {
    }

    public static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    /** Slabs must start at 1+, not overlap, and only the highest may be open-ended. */
    public static List<DriverPaySlab> validateSlabs(List<DriverPaySlab> slabs) {
        List<DriverPaySlab> sorted = new ArrayList<>(slabs);
        sorted.sort(Comparator.comparing(DriverPaySlab::getTripsFrom, Comparator.nullsFirst(Integer::compareTo)));
        Integer previousTo = null;
        for (int i = 0; i < sorted.size(); i++) {
            DriverPaySlab s = sorted.get(i);
            if (s.getTripsFrom() == null || s.getTripsFrom() < 1) {
                throw invalid("Every slab must start at 1 trip or more.");
            }
            if (s.getTripsTo() != null && s.getTripsTo() < s.getTripsFrom()) {
                throw invalid("Slab " + s.getTripsFrom() + "-" + s.getTripsTo() + ": 'trips to' is less than 'trips from'.");
            }
            if (s.getDailyAmount() == null || s.getDailyAmount().signum() < 0) {
                throw invalid("Daily amount cannot be empty or negative.");
            }
            if (s.getTripsTo() == null && i < sorted.size() - 1) {
                throw invalid("Only the highest slab can be open-ended (no 'trips to').");
            }
            if (i > 0 && (previousTo == null || s.getTripsFrom() <= previousTo)) {
                throw invalid("Slabs overlap at " + s.getTripsFrom() + " trips.");
            }
            s.setDailyAmount(money(s.getDailyAmount()));
            previousTo = s.getTripsTo();
        }
        return sorted;
    }

    /** The slab a day's trip count falls in, or null (0 trips or a gap in the slabs). */
    public static DriverPaySlab slabFor(int tripCount, List<DriverPaySlab> slabs) {
        if (tripCount <= 0) return null;
        for (DriverPaySlab s : slabs) {
            if (tripCount >= s.getTripsFrom() && (s.getTripsTo() == null || tripCount <= s.getTripsTo())) {
                return s;
            }
        }
        return null;
    }

    /** @param tripsByDate completed trips per business date (days without trips may be absent) */
    public static Result calculate(Map<LocalDate, Long> tripsByDate, List<DriverPaySlab> slabs) {
        List<Day> days = new ArrayList<>();
        int totalTrips = 0;
        int tripDays = 0;
        BigDecimal earnings = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, Long> e : new TreeMap<>(tripsByDate).entrySet()) {
            int count = e.getValue() == null ? 0 : e.getValue().intValue();
            if (count <= 0) continue;
            DriverPaySlab slab = slabFor(count, slabs);
            BigDecimal amount = slab == null ? BigDecimal.ZERO.setScale(2) : money(slab.getDailyAmount());
            days.add(new Day(e.getKey(), count, slab == null ? null : slab.getTripsFrom(),
                    slab == null ? null : slab.getTripsTo(), amount));
            totalTrips += count;
            tripDays++;
            earnings = earnings.add(amount);
        }
        return new Result(days, totalTrips, tripDays, money(earnings));
    }

    /** gross - deductions - advance; each part must be >= 0 and the net cannot go below zero. */
    public static BigDecimal net(BigDecimal gross, BigDecimal deductions, BigDecimal advance) {
        BigDecimal g = money(gross), d = money(deductions), a = money(advance);
        if (d.signum() < 0 || a.signum() < 0) {
            throw invalid("Deductions and advance recovery cannot be negative.");
        }
        BigDecimal net = g.subtract(d).subtract(a);
        if (net.signum() < 0) {
            throw new BusinessValidationException("Deductions Exceed Earnings", "PAYROLL_NET_NEGATIVE",
                    String.format("Deductions ₹%s + advance ₹%s are more than gross earnings ₹%s.",
                            d.toPlainString(), a.toPlainString(), g.toPlainString()),
                    "Reduce the advance recovery or deductions; the rest of the advance stays outstanding for next month.");
        }
        return net;
    }

    private static BusinessValidationException invalid(String msg) {
        return new BusinessValidationException("Invalid Pay Slab", "PAY_SLAB_INVALID", msg,
                "Fix the daily pay slabs, e.g. 1-1 trips = 500 and 2+ trips = 1000.");
    }
}
