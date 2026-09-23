package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceDueDashboardItem;
import com.transport.erp.dto.MaintenanceDueDashboardResponse;
import com.transport.erp.dto.MaintenanceDueDashboardSummary;
import com.transport.erp.dto.MaintenanceDueResponse;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.MaintenanceRule;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleMaintenanceBaseline;
import com.transport.erp.repository.MaintenanceRuleRepository;
import com.transport.erp.repository.VehicleMaintenanceBaselineRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phase 3 read-only dashboard projection over the locked Phase 2 due calculator.
 * Does not change GET /api/v1/maintenance/due or MaintenanceDueResponse.
 */
@Service
public class MaintenanceDueDashboardService {

    public static final int ALERT_CAP = 20;

    private static final Comparator<MaintenanceDueDashboardItem> ALERT_ORDER =
            Comparator.comparingInt((MaintenanceDueDashboardItem item) -> statusRank(item.getDueStatus()))
                    .thenComparing(MaintenanceDueDashboardItem::getRemainingKm,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(MaintenanceDueDashboardItem::getRemainingDays,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(MaintenanceDueDashboardItem::getVehicleId,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(MaintenanceDueDashboardItem::getMaintenanceType,
                            Comparator.nullsLast(String::compareTo));

    @Autowired
    private TenantAccessService tenantAccess;
    @Autowired
    private MaintenanceRuleRepository ruleRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private VehicleMaintenanceBaselineRepository baselineRepository;

    @Transactional(readOnly = true)
    public MaintenanceDueDashboardResponse build(Long companyId, LocalDate today) {
        if (companyId == null) {
            return MaintenanceDueDashboardResponse.empty();
        }
        LocalDate asOf = today != null ? today : LocalDate.now();
        AppUser user = tenantAccess.requireCurrentUser();

        List<MaintenanceRule> rules = ruleRepository.findByCompanyIdAndStatusAndIsDeletedFalse(companyId, "ACTIVE");
        if (rules.isEmpty()) {
            return MaintenanceDueDashboardResponse.empty();
        }

        List<Vehicle> vehicles = loadVehicles(companyId, user);
        if (vehicles.isEmpty()) {
            return MaintenanceDueDashboardResponse.empty();
        }

        List<Long> ruleIds = rules.stream().map(MaintenanceRule::getId).toList();
        Map<String, VehicleMaintenanceBaseline> baselines = baselineRepository
                .findByCompanyIdAndIsDeletedFalseAndRule_IdIn(companyId, ruleIds)
                .stream()
                .collect(Collectors.toMap(
                        b -> b.getRule().getId() + ":" + b.getVehicle().getId(),
                        Function.identity(),
                        (a, b) -> a));

        long overdueCount = 0;
        long dueCount = 0;
        long dueSoonCount = 0;
        long unknownCount = 0;
        List<MaintenanceDueDashboardItem> actionable = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            for (MaintenanceRule rule : rules) {
                VehicleMaintenanceBaseline baseline = baselines.get(rule.getId() + ":" + vehicle.getId());
                MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle, baseline, asOf);
                String status = row.getDueStatus();
                if (MaintenanceDueCalculator.OVERDUE.equals(status)) {
                    overdueCount++;
                } else if (MaintenanceDueCalculator.DUE.equals(status)) {
                    dueCount++;
                } else if (MaintenanceDueCalculator.DUE_SOON.equals(status)) {
                    dueSoonCount++;
                } else if (MaintenanceDueCalculator.UNKNOWN.equals(status)) {
                    unknownCount++;
                } else {
                    continue;
                }
                actionable.add(toItem(row, vehicle));
            }
        }

        actionable.sort(ALERT_ORDER);
        int limit = Math.min(ALERT_CAP, actionable.size());

        MaintenanceDueDashboardResponse response = new MaintenanceDueDashboardResponse();
        MaintenanceDueDashboardSummary summary = response.getSummary();
        summary.setOverdueCount(overdueCount);
        summary.setDueCount(dueCount);
        summary.setDueSoonCount(dueSoonCount);
        summary.setUnknownCount(unknownCount);
        response.setAlerts(new ArrayList<>(actionable.subList(0, limit)));
        return response;
    }

    private List<Vehicle> loadVehicles(Long companyId, AppUser user) {
        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdAndIsDeletedFalseOrderByIdAsc(companyId);
        Long branchFilter = user != null ? user.getBranchId() : null;
        if (branchFilter == null) {
            return vehicles;
        }
        return vehicles.stream()
                .filter(v -> v.getBranchId() == null || branchFilter.equals(v.getBranchId()))
                .collect(Collectors.toList());
    }

    private static MaintenanceDueDashboardItem toItem(MaintenanceDueResponse row, Vehicle vehicle) {
        MaintenanceDueDashboardItem item = new MaintenanceDueDashboardItem();
        item.setVehicleId(row.getVehicleId());
        item.setRuleId(row.getRuleId());
        item.setVehicleCode(vehicle.getCode());
        item.setVehicleName(vehicle.getName());
        item.setMaintenanceType(row.getMaintenanceType());
        item.setTriggerMode(row.getTriggerMode());
        item.setCurrentOdometerKm(row.getCurrentOdometerKm());
        item.setLastServiceKm(row.getLastServiceKm());
        item.setLastServiceDate(row.getLastServiceDate());
        item.setNextDueKm(row.getNextDueKm());
        item.setNextDueDate(row.getNextDueDate());
        item.setRemainingKm(row.getRemainingKm());
        item.setRemainingDays(row.getRemainingDays());
        item.setDueStatus(row.getDueStatus());
        return item;
    }

    private static int statusRank(String status) {
        if (MaintenanceDueCalculator.OVERDUE.equals(status)) {
            return 0;
        }
        if (MaintenanceDueCalculator.DUE.equals(status)) {
            return 1;
        }
        if (MaintenanceDueCalculator.DUE_SOON.equals(status)) {
            return 2;
        }
        if (MaintenanceDueCalculator.UNKNOWN.equals(status)) {
            return 3;
        }
        return 4;
    }
}
