package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceDueResponse;
import com.transport.erp.model.MaintenanceRule;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleMaintenanceBaseline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure read-time due calculator. No repository access.
 */
public final class MaintenanceDueCalculator {

    public static final String UNKNOWN = "UNKNOWN";
    public static final String NOT_DUE = "NOT_DUE";
    public static final String DUE_SOON = "DUE_SOON";
    public static final String DUE = "DUE";
    public static final String OVERDUE = "OVERDUE";

    public static final String MODE_KM = "KM";
    public static final String MODE_DAYS = "DAYS";
    public static final String MODE_KM_OR_DAYS = "KM_OR_DAYS";

    static final BigDecimal DEFAULT_DUE_SOON_KM = new BigDecimal("1000");
    static final int DEFAULT_DUE_SOON_DAYS = 14;

    private MaintenanceDueCalculator() {
    }

    public static MaintenanceDueResponse calculate(
            MaintenanceRule rule,
            Vehicle vehicle,
            VehicleMaintenanceBaseline baseline,
            LocalDate today) {
        MaintenanceDueResponse response = new MaintenanceDueResponse();
        response.setVehicleId(vehicle.getId());
        response.setRuleId(rule.getId());
        response.setMaintenanceType(rule.getMaintenanceType());
        response.setTriggerMode(rule.getTriggerMode());
        response.setCurrentOdometerKm(vehicle.getCurrentOdometerKm());
        if (baseline != null) {
            response.setLastServiceKm(baseline.getLastServiceKm());
            response.setLastServiceDate(baseline.getLastServiceDate());
        }

        String mode = rule.getTriggerMode();
        boolean kmApplicable = MODE_KM.equals(mode) || MODE_KM_OR_DAYS.equals(mode);
        boolean daysApplicable = MODE_DAYS.equals(mode) || MODE_KM_OR_DAYS.equals(mode);
        if (MODE_KM_OR_DAYS.equals(mode)) {
            kmApplicable = kmApplicable && isPositive(rule.getIntervalKm());
            daysApplicable = daysApplicable && isPositiveDays(rule.getIntervalDays());
        }

        String kmStatus = null;
        String dateStatus = null;

        if (kmApplicable) {
            kmStatus = applyKm(rule, vehicle, baseline, response);
        }
        if (daysApplicable) {
            dateStatus = applyDays(rule, baseline, today, response);
        }

        response.setDueStatus(combine(kmStatus, dateStatus));
        return response;
    }

    private static String applyKm(
            MaintenanceRule rule,
            Vehicle vehicle,
            VehicleMaintenanceBaseline baseline,
            MaintenanceDueResponse response) {
        if (baseline == null || baseline.getLastServiceKm() == null || !isPositive(rule.getIntervalKm())) {
            return UNKNOWN;
        }
        if (vehicle.getCurrentOdometerKm() == null) {
            response.setNextDueKm(baseline.getLastServiceKm().add(rule.getIntervalKm()));
            return UNKNOWN;
        }
        BigDecimal nextDueKm = baseline.getLastServiceKm().add(rule.getIntervalKm());
        BigDecimal remainingKm = nextDueKm.subtract(vehicle.getCurrentOdometerKm());
        response.setNextDueKm(nextDueKm);
        response.setRemainingKm(remainingKm);
        BigDecimal dueSoon = rule.getDueSoonKm() != null ? rule.getDueSoonKm() : DEFAULT_DUE_SOON_KM;
        return statusFromRemaining(remainingKm.compareTo(BigDecimal.ZERO), remainingKm.compareTo(dueSoon));
    }

    private static String applyDays(
            MaintenanceRule rule,
            VehicleMaintenanceBaseline baseline,
            LocalDate today,
            MaintenanceDueResponse response) {
        if (baseline == null || baseline.getLastServiceDate() == null || !isPositiveDays(rule.getIntervalDays())) {
            return UNKNOWN;
        }
        LocalDate nextDueDate = baseline.getLastServiceDate().plusDays(rule.getIntervalDays());
        long remainingDays = ChronoUnit.DAYS.between(today, nextDueDate);
        response.setNextDueDate(nextDueDate);
        response.setRemainingDays(remainingDays);
        int dueSoon = rule.getDueSoonDays() != null ? rule.getDueSoonDays() : DEFAULT_DUE_SOON_DAYS;
        int vsZero = Long.compare(remainingDays, 0);
        int vsSoon = Long.compare(remainingDays, dueSoon);
        return statusFromRemaining(vsZero, vsSoon);
    }

    /**
     * vsZero: remaining compared to 0. vsSoon: remaining compared to due-soon threshold.
     */
    static String statusFromRemaining(int vsZero, int vsSoon) {
        if (vsZero > 0 && vsSoon > 0) {
            return NOT_DUE;
        }
        if (vsZero > 0) {
            return DUE_SOON;
        }
        if (vsZero == 0) {
            return DUE;
        }
        return OVERDUE;
    }

    static String combine(String kmStatus, String dateStatus) {
        if (kmStatus == null) {
            return dateStatus != null ? dateStatus : UNKNOWN;
        }
        if (dateStatus == null) {
            return kmStatus;
        }
        if (UNKNOWN.equals(kmStatus)) {
            return dateStatus;
        }
        if (UNKNOWN.equals(dateStatus)) {
            return kmStatus;
        }
        return rank(kmStatus) >= rank(dateStatus) ? kmStatus : dateStatus;
    }

    private static int rank(String status) {
        if (OVERDUE.equals(status)) {
            return 4;
        }
        if (DUE.equals(status)) {
            return 3;
        }
        if (DUE_SOON.equals(status)) {
            return 2;
        }
        if (NOT_DUE.equals(status)) {
            return 1;
        }
        return 0;
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean isPositiveDays(Integer value) {
        return value != null && value > 0;
    }
}
