package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.DriverPaySlab;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Wheel-based daily pay (V88): highest-paying lorry type per day, "All vehicles" fallback, unchanged without wheel slabs. */
class DriverPayrollWheelTest {
    private static final LocalDate D1 = LocalDate.of(2026, 10, 1), D2 = LocalDate.of(2026, 10, 2);

    private static DriverPaySlab slab(String wheel, int from, Integer to, int amount) {
        DriverPaySlab s = new DriverPaySlab(); s.setWheelCount(wheel); s.setTripsFrom(from); s.setTripsTo(to); s.setDailyAmount(BigDecimal.valueOf(amount)); return s;
    }
    private static List<DriverPaySlab> slabs() {
        return DriverPayrollCalculator.validateSlabs(new ArrayList<>(List.of(
                slab(null, 1, 1, 500), slab(null, 2, null, 1000),
                slab("6", 1, 1, 400), slab("6", 2, null, 800),
                slab("10", 1, 1, 600), slab("10", 2, 2, 1200), slab("10", 3, null, 1500))));
    }
    private static DriverPayrollCalculator.TripRef t(LocalDate d, String w) { return new DriverPayrollCalculator.TripRef(d, w); }

    @Test void highestPayingTypeOfTheDayPaysOnce() {
        var r = DriverPayrollCalculator.calculateByWheel(List.of(t(D1, "6"), t(D1, "10")), slabs());
        assertEquals(1, r.days().size());
        assertEquals(new BigDecimal("1200.00"), r.days().get(0).amount());   // 2 trips, 10W slab beats 6W (800)
        assertEquals("10", r.days().get(0).wheel());
    }
    @Test void typeWithoutOwnSlabsUsesAllVehicles() {
        var r = DriverPayrollCalculator.calculateByWheel(List.of(t(D1, "14")), slabs());
        assertEquals(new BigDecimal("500.00"), r.tripEarnings());
        assertNull(r.days().get(0).wheel());
    }
    @Test void blankWheelUsesAllVehicles() {
        var r = DriverPayrollCalculator.calculateByWheel(List.of(t(D1, null), t(D1, null)), slabs());
        assertEquals(new BigDecimal("1000.00"), r.tripEarnings());
    }
    @Test void onlyAllVehicleSlabsGiveTheSameResultAsBefore() {
        List<DriverPaySlab> old = DriverPayrollCalculator.validateSlabs(new ArrayList<>(List.of(slab(null, 1, 1, 500), slab(null, 2, null, 1000))));
        var byWheel = DriverPayrollCalculator.calculateByWheel(List.of(t(D1, "10"), t(D1, "6"), t(D2, "10")), old);
        var before = DriverPayrollCalculator.calculate(Map.of(D1, 2L, D2, 1L), old);
        assertEquals(before.tripEarnings(), byWheel.tripEarnings());
        assertEquals(before.totalTrips(), byWheel.totalTrips());
        assertEquals(before.tripDays(), byWheel.tripDays());
    }
    @Test void slabGroupsAreValidatedSeparately() {
        // the same trip ranges in two groups are fine; overlap inside one group is refused
        assertDoesNotThrow(() -> DriverPayrollCalculator.validateSlabs(new ArrayList<>(List.of(slab(null, 1, null, 500), slab("10", 1, null, 600)))));
        assertThrows(BusinessValidationException.class,
                () -> DriverPayrollCalculator.validateSlabs(new ArrayList<>(List.of(slab("10", 1, 2, 600), slab("10", 2, null, 900)))));
    }
    @Test void wheelValuesAreNormalised() {
        assertEquals("10", DriverPayrollCalculator.normWheel("10W"));
        assertEquals("10", DriverPayrollCalculator.normWheel(" 10 wheel "));
        assertEquals("EQUIPMENT", DriverPayrollCalculator.normWheel("equipment"));
        assertNull(DriverPayrollCalculator.normWheel(""));
        assertThrows(BusinessValidationException.class, () -> DriverPayrollCalculator.normWheel("7"));
    }
}
