package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceDueResponse;
import com.transport.erp.model.MaintenanceRule;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleMaintenanceBaseline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceDueCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);

    @Test
    @DisplayName("KM remaining 2000 with dueSoon 1000 is NOT_DUE")
    void kmNotDue() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmRule(), vehicle(new BigDecimal("48000")), baselineKm(new BigDecimal("40000")), TODAY);
        assertEquals(0, new BigDecimal("50000").compareTo(row.getNextDueKm()));
        assertEquals(0, new BigDecimal("2000").compareTo(row.getRemainingKm()));
        assertEquals(MaintenanceDueCalculator.NOT_DUE, row.getDueStatus());
    }

    @Test
    @DisplayName("KM remaining 800 with dueSoon 1000 is DUE_SOON")
    void kmDueSoon() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmRule(), vehicle(new BigDecimal("49200")), baselineKm(new BigDecimal("40000")), TODAY);
        assertEquals(MaintenanceDueCalculator.DUE_SOON, row.getDueStatus());
    }

    @Test
    @DisplayName("KM remaining 0 is DUE")
    void kmDue() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmRule(), vehicle(new BigDecimal("50000")), baselineKm(new BigDecimal("40000")), TODAY);
        assertEquals(MaintenanceDueCalculator.DUE, row.getDueStatus());
    }

    @Test
    @DisplayName("KM remaining -1 is OVERDUE")
    void kmOverdue() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmRule(), vehicle(new BigDecimal("50001")), baselineKm(new BigDecimal("40000")), TODAY);
        assertEquals(MaintenanceDueCalculator.OVERDUE, row.getDueStatus());
    }

    @Test
    @DisplayName("KM rule without current odometer is UNKNOWN")
    void kmNoOdometer() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmRule(), vehicle(null), baselineKm(new BigDecimal("40000")), TODAY);
        assertEquals(MaintenanceDueCalculator.UNKNOWN, row.getDueStatus());
        assertEquals(0, new BigDecimal("50000").compareTo(row.getNextDueKm()));
        assertNull(row.getRemainingKm());
    }

    @Test
    @DisplayName("KM rule without baseline is UNKNOWN")
    void kmNoBaseline() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmRule(), vehicle(new BigDecimal("48000")), null, TODAY);
        assertEquals(MaintenanceDueCalculator.UNKNOWN, row.getDueStatus());
    }

    @Test
    @DisplayName("DAYS remaining above threshold is NOT_DUE")
    void daysNotDue() {
        MaintenanceRule rule = daysRule(60, 14);
        VehicleMaintenanceBaseline baseline = baselineDate(LocalDate.of(2026, 8, 1));
        // nextDue = 2026-09-30, remaining 9 — wait that's DUE_SOON. Use longer interval.
        rule.setIntervalDays(90);
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle(null), baseline, TODAY);
        // nextDue 2026-10-30 remaining 39 > 14
        assertEquals(MaintenanceDueCalculator.NOT_DUE, row.getDueStatus());
        assertTrue(row.getRemainingDays() > 14);
    }

    @Test
    @DisplayName("DAYS remaining within threshold is DUE_SOON")
    void daysDueSoon() {
        MaintenanceRule rule = daysRule(60, 14);
        VehicleMaintenanceBaseline baseline = baselineDate(LocalDate.of(2026, 8, 1));
        // nextDue 2026-09-30 remaining 9
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle(null), baseline, TODAY);
        assertEquals(9L, row.getRemainingDays());
        assertEquals(MaintenanceDueCalculator.DUE_SOON, row.getDueStatus());
    }

    @Test
    @DisplayName("DAYS remaining 0 is DUE")
    void daysDue() {
        MaintenanceRule rule = daysRule(14, 14);
        VehicleMaintenanceBaseline baseline = baselineDate(LocalDate.of(2026, 9, 7));
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle(null), baseline, TODAY);
        assertEquals(0L, row.getRemainingDays());
        assertEquals(MaintenanceDueCalculator.DUE, row.getDueStatus());
    }

    @Test
    @DisplayName("DAYS remaining negative is OVERDUE")
    void daysOverdue() {
        MaintenanceRule rule = daysRule(30, 14);
        VehicleMaintenanceBaseline baseline = baselineDate(LocalDate.of(2026, 8, 1));
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle(null), baseline, TODAY);
        assertTrue(row.getRemainingDays() < 0);
        assertEquals(MaintenanceDueCalculator.OVERDUE, row.getDueStatus());
    }

    @Test
    @DisplayName("KM_OR_DAYS uses the more severe dimension")
    void kmOrDaysDateMoreSevere() {
        MaintenanceRule rule = kmOrDaysRule();
        Vehicle vehicle = vehicle(new BigDecimal("49200")); // KM DUE_SOON
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setLastServiceKm(new BigDecimal("40000"));
        baseline.setLastServiceDate(LocalDate.of(2026, 8, 1)); // date OVERDUE with 30 days
        rule.setIntervalDays(30);
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle, baseline, TODAY);
        assertEquals(MaintenanceDueCalculator.OVERDUE, row.getDueStatus());
    }

    @Test
    @DisplayName("KM_OR_DAYS uses KM when KM is more severe")
    void kmOrDaysKmMoreSevere() {
        MaintenanceRule rule = kmOrDaysRule();
        rule.setIntervalDays(90);
        Vehicle vehicle = vehicle(new BigDecimal("50001")); // KM OVERDUE
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setLastServiceKm(new BigDecimal("40000"));
        baseline.setLastServiceDate(LocalDate.of(2026, 8, 1)); // date NOT_DUE
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle, baseline, TODAY);
        assertEquals(MaintenanceDueCalculator.OVERDUE, row.getDueStatus());
    }

    @Test
    @DisplayName("KM_OR_DAYS same status remains that status")
    void kmOrDaysSameStatus() {
        MaintenanceRule rule = kmOrDaysRule();
        Vehicle vehicle = vehicle(new BigDecimal("50000"));
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setLastServiceKm(new BigDecimal("40000"));
        baseline.setLastServiceDate(LocalDate.of(2026, 9, 7));
        rule.setIntervalDays(14);
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle, baseline, TODAY);
        assertEquals(MaintenanceDueCalculator.DUE, row.getDueStatus());
    }

    @Test
    @DisplayName("KM_OR_DAYS uses the valid dimension when the other is unavailable")
    void kmOrDaysOneDimensionUnavailable() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmOrDaysRule(), vehicle(new BigDecimal("49200")), baselineKm(new BigDecimal("40000")), TODAY);
        assertEquals(MaintenanceDueCalculator.DUE_SOON, row.getDueStatus());
        assertNull(row.getNextDueDate());
    }

    @Test
    @DisplayName("KM_OR_DAYS with no baseline is UNKNOWN")
    void kmOrDaysBothUnavailable() {
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                kmOrDaysRule(), vehicle(new BigDecimal("49200")), null, TODAY);
        assertEquals(MaintenanceDueCalculator.UNKNOWN, row.getDueStatus());
    }

    @Test
    @DisplayName("DAYS-only ignores missing odometer")
    void daysIgnoresNullOdometer() {
        MaintenanceRule rule = daysRule(90, 14);
        MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(
                rule, vehicle(null), baselineDate(LocalDate.of(2026, 8, 1)), TODAY);
        assertEquals(MaintenanceDueCalculator.NOT_DUE, row.getDueStatus());
    }

    private MaintenanceRule kmRule() {
        MaintenanceRule rule = new MaintenanceRule();
        rule.setId(1L);
        rule.setMaintenanceType("OIL_CHANGE");
        rule.setTriggerMode(MaintenanceDueCalculator.MODE_KM);
        rule.setIntervalKm(new BigDecimal("10000"));
        rule.setDueSoonKm(new BigDecimal("1000"));
        return rule;
    }

    private MaintenanceRule daysRule(int intervalDays, int dueSoonDays) {
        MaintenanceRule rule = new MaintenanceRule();
        rule.setId(2L);
        rule.setMaintenanceType("ENGINE_SERVICE");
        rule.setTriggerMode(MaintenanceDueCalculator.MODE_DAYS);
        rule.setIntervalDays(intervalDays);
        rule.setDueSoonDays(dueSoonDays);
        return rule;
    }

    private MaintenanceRule kmOrDaysRule() {
        MaintenanceRule rule = new MaintenanceRule();
        rule.setId(3L);
        rule.setMaintenanceType("GENERAL_SERVICE");
        rule.setTriggerMode(MaintenanceDueCalculator.MODE_KM_OR_DAYS);
        rule.setIntervalKm(new BigDecimal("10000"));
        rule.setIntervalDays(60);
        rule.setDueSoonKm(new BigDecimal("1000"));
        rule.setDueSoonDays(14);
        return rule;
    }

    private Vehicle vehicle(BigDecimal km) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setCurrentOdometerKm(km);
        return vehicle;
    }

    private VehicleMaintenanceBaseline baselineKm(BigDecimal km) {
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setLastServiceKm(km);
        return baseline;
    }

    private VehicleMaintenanceBaseline baselineDate(LocalDate date) {
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setLastServiceDate(date);
        return baseline;
    }
}
