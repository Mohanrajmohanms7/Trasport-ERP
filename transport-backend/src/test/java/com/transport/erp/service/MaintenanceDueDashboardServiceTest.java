package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceDueDashboardItem;
import com.transport.erp.dto.MaintenanceDueDashboardResponse;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.MaintenanceRule;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleMaintenanceBaseline;
import com.transport.erp.repository.MaintenanceRuleRepository;
import com.transport.erp.repository.VehicleMaintenanceBaselineRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintenanceDueDashboardServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);
    private static final Long COMPANY_ID = 3L;

    @Mock private TenantAccessService tenantAccess;
    @Mock private MaintenanceRuleRepository ruleRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleMaintenanceBaselineRepository baselineRepository;

    @InjectMocks
    private MaintenanceDueDashboardService service;

    private AppUser companyAdmin;

    @BeforeEach
    void setUp() {
        companyAdmin = user("admin", COMPANY_ID, null);
        when(tenantAccess.requireCurrentUser()).thenReturn(companyAdmin);
        when(ruleRepository.findByCompanyIdAndStatusAndIsDeletedFalse(COMPANY_ID, "ACTIVE"))
                .thenReturn(List.of());
        when(vehicleRepository.findByCompanyIdAndIsDeletedFalseOrderByIdAsc(COMPANY_ID))
                .thenReturn(List.of());
        when(baselineRepository.findByCompanyIdAndIsDeletedFalseAndRule_IdIn(eq(COMPANY_ID), any()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Null company returns empty dashboard without repository loads")
    void superAdminWithoutCompanyIsEmpty() {
        MaintenanceDueDashboardResponse response = service.build(null, TODAY);
        assertEquals(0, response.getSummary().getOverdueCount());
        assertEquals(0, response.getSummary().getDueCount());
        assertEquals(0, response.getSummary().getDueSoonCount());
        assertEquals(0, response.getSummary().getUnknownCount());
        assertTrue(response.getAlerts().isEmpty());
        verify(ruleRepository, never()).findByCompanyIdAndStatusAndIsDeletedFalse(any(), any());
        verify(vehicleRepository, never()).findByCompanyIdAndIsDeletedFalseOrderByIdAsc(any());
    }

    @Test
    @DisplayName("Counts cover OVERDUE DUE DUE_SOON UNKNOWN and exclude NOT_DUE from alerts")
    void countsAndNotDueExclusion() {
        MaintenanceRule rule = kmRule(1L, "OIL_CHANGE");
        Vehicle overdue = vehicle(10L, "TN01AA0001", "Overdue Truck", new BigDecimal("51000"), 1L);
        Vehicle due = vehicle(11L, "TN01AA0002", "Due Truck", new BigDecimal("50000"), 1L);
        Vehicle dueSoon = vehicle(12L, "TN01AA0003", "Soon Truck", new BigDecimal("49200"), 1L);
        Vehicle unknown = vehicle(13L, "TN01AA0004", "Unknown Truck", null, 1L);
        Vehicle notDue = vehicle(14L, "TN01AA0005", "Healthy Truck", new BigDecimal("41000"), 1L);

        stubCompanyData(List.of(rule), List.of(overdue, due, dueSoon, unknown, notDue), List.of(
                baseline(rule, overdue, new BigDecimal("40000")),
                baseline(rule, due, new BigDecimal("40000")),
                baseline(rule, dueSoon, new BigDecimal("40000")),
                baseline(rule, unknown, new BigDecimal("40000")),
                baseline(rule, notDue, new BigDecimal("40000"))
        ));

        MaintenanceDueDashboardResponse response = service.build(COMPANY_ID, TODAY);
        assertEquals(1, response.getSummary().getOverdueCount());
        assertEquals(1, response.getSummary().getDueCount());
        assertEquals(1, response.getSummary().getDueSoonCount());
        assertEquals(1, response.getSummary().getUnknownCount());
        assertEquals(4, response.getAlerts().size());
        assertTrue(response.getAlerts().stream().noneMatch(
                item -> MaintenanceDueCalculator.NOT_DUE.equals(item.getDueStatus())));
        assertEquals("TN01AA0001", response.getAlerts().get(0).getVehicleCode());
        assertEquals("Overdue Truck", response.getAlerts().get(0).getVehicleName());
    }

    @Test
    @DisplayName("Alerts are ordered OVERDUE then DUE then DUE_SOON then UNKNOWN")
    void severityOrdering() {
        MaintenanceRule rule = kmRule(1L, "OIL_CHANGE");
        Vehicle unknown = vehicle(13L, "U-13", "Unknown", null, 1L);
        Vehicle dueSoon = vehicle(12L, "S-12", "Soon", new BigDecimal("49200"), 1L);
        Vehicle overdue = vehicle(10L, "O-10", "Overdue", new BigDecimal("51000"), 1L);
        Vehicle due = vehicle(11L, "D-11", "Due", new BigDecimal("50000"), 1L);

        stubCompanyData(List.of(rule), List.of(unknown, dueSoon, overdue, due), List.of(
                baseline(rule, unknown, new BigDecimal("40000")),
                baseline(rule, dueSoon, new BigDecimal("40000")),
                baseline(rule, overdue, new BigDecimal("40000")),
                baseline(rule, due, new BigDecimal("40000"))
        ));

        List<String> statuses = service.build(COMPANY_ID, TODAY).getAlerts().stream()
                .map(MaintenanceDueDashboardItem::getDueStatus)
                .toList();
        assertEquals(List.of(
                MaintenanceDueCalculator.OVERDUE,
                MaintenanceDueCalculator.DUE,
                MaintenanceDueCalculator.DUE_SOON,
                MaintenanceDueCalculator.UNKNOWN
        ), statuses);
        assertEquals(1L, service.build(COMPANY_ID, TODAY).getAlerts().get(0).getRuleId());
    }

    @Test
    @DisplayName("Same-status alerts order by remaining KM then vehicle id")
    void deterministicSecondaryOrdering() {
        MaintenanceRule rule = kmRule(1L, "OIL_CHANGE");
        Vehicle later = vehicle(22L, "B-22", "Less overdue", new BigDecimal("50500"), 1L);
        Vehicle sooner = vehicle(21L, "A-21", "More overdue", new BigDecimal("52000"), 1L);

        stubCompanyData(List.of(rule), List.of(later, sooner), List.of(
                baseline(rule, later, new BigDecimal("40000")),
                baseline(rule, sooner, new BigDecimal("40000"))
        ));

        List<MaintenanceDueDashboardItem> alerts = service.build(COMPANY_ID, TODAY).getAlerts();
        assertEquals(2, alerts.size());
        assertEquals(21L, alerts.get(0).getVehicleId());
        assertEquals(22L, alerts.get(1).getVehicleId());
        assertTrue(alerts.get(0).getRemainingKm().compareTo(alerts.get(1).getRemainingKm()) < 0);
    }

    @Test
    @DisplayName("Alert cap does not reduce full-population counts")
    void alertCapDoesNotAffectCounts() {
        MaintenanceRule rule = kmRule(1L, "OIL_CHANGE");
        List<Vehicle> vehicles = new ArrayList<>();
        List<VehicleMaintenanceBaseline> baselines = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Vehicle vehicle = vehicle((long) i, "V-" + i, "Truck " + i, new BigDecimal("51000"), 1L);
            vehicles.add(vehicle);
            baselines.add(baseline(rule, vehicle, new BigDecimal("40000")));
        }
        stubCompanyData(List.of(rule), vehicles, baselines);

        MaintenanceDueDashboardResponse response = service.build(COMPANY_ID, TODAY);
        assertEquals(25, response.getSummary().getOverdueCount());
        assertEquals(0, response.getSummary().getDueCount());
        assertEquals(MaintenanceDueDashboardService.ALERT_CAP, response.getAlerts().size());
        assertEquals(20, response.getAlerts().size());
    }

    @Test
    @DisplayName("Dashboard queries only the resolved company id")
    void companyIsolation() {
        MaintenanceRule rule = kmRule(1L, "OIL_CHANGE");
        Vehicle vehicle = vehicle(10L, "TN01", "Own", new BigDecimal("51000"), 1L);
        stubCompanyData(List.of(rule), List.of(vehicle), List.of(baseline(rule, vehicle, new BigDecimal("40000"))));

        service.build(COMPANY_ID, TODAY);

        verify(ruleRepository).findByCompanyIdAndStatusAndIsDeletedFalse(COMPANY_ID, "ACTIVE");
        verify(vehicleRepository).findByCompanyIdAndIsDeletedFalseOrderByIdAsc(COMPANY_ID);
        verify(ruleRepository, never()).findByCompanyIdAndStatusAndIsDeletedFalse(eq(99L), any());
        verify(vehicleRepository, never()).findByCompanyIdAndIsDeletedFalseOrderByIdAsc(99L);
    }

    @Test
    @DisplayName("Branch-scoped users only see their branch vehicles")
    void branchIsolation() {
        AppUser branchManager = user("branch", COMPANY_ID, 2L);
        when(tenantAccess.requireCurrentUser()).thenReturn(branchManager);

        MaintenanceRule rule = kmRule(1L, "OIL_CHANGE");
        Vehicle otherBranch = vehicle(10L, "B1", "Other", new BigDecimal("51000"), 1L);
        Vehicle ownBranch = vehicle(11L, "B2", "Own", new BigDecimal("51000"), 2L);
        Vehicle unassigned = vehicle(12L, "B0", "Unassigned", new BigDecimal("51000"), null);
        stubCompanyData(List.of(rule), List.of(otherBranch, ownBranch, unassigned), List.of(
                baseline(rule, otherBranch, new BigDecimal("40000")),
                baseline(rule, ownBranch, new BigDecimal("40000")),
                baseline(rule, unassigned, new BigDecimal("40000"))
        ));

        MaintenanceDueDashboardResponse response = service.build(COMPANY_ID, TODAY);
        assertEquals(2, response.getSummary().getOverdueCount());
        List<Long> vehicleIds = response.getAlerts().stream()
                .map(MaintenanceDueDashboardItem::getVehicleId)
                .toList();
        assertEquals(List.of(11L, 12L), vehicleIds);
        assertFalse(vehicleIds.contains(10L));
    }

    @Test
    @DisplayName("Company admin without branch sees all company vehicles")
    void companyAdminSeesAllBranches() {
        MaintenanceRule rule = kmRule(1L, "OIL_CHANGE");
        Vehicle branchOne = vehicle(10L, "B1", "One", new BigDecimal("51000"), 1L);
        Vehicle branchTwo = vehicle(11L, "B2", "Two", new BigDecimal("51000"), 2L);
        stubCompanyData(List.of(rule), List.of(branchOne, branchTwo), List.of(
                baseline(rule, branchOne, new BigDecimal("40000")),
                baseline(rule, branchTwo, new BigDecimal("40000"))
        ));

        MaintenanceDueDashboardResponse response = service.build(COMPANY_ID, TODAY);
        assertEquals(2, response.getSummary().getOverdueCount());
        assertEquals(2, response.getAlerts().size());
    }

    private void stubCompanyData(
            List<MaintenanceRule> rules,
            List<Vehicle> vehicles,
            List<VehicleMaintenanceBaseline> baselines) {
        when(ruleRepository.findByCompanyIdAndStatusAndIsDeletedFalse(COMPANY_ID, "ACTIVE")).thenReturn(rules);
        when(vehicleRepository.findByCompanyIdAndIsDeletedFalseOrderByIdAsc(COMPANY_ID)).thenReturn(vehicles);
        when(baselineRepository.findByCompanyIdAndIsDeletedFalseAndRule_IdIn(eq(COMPANY_ID), any()))
                .thenReturn(baselines);
    }

    private static MaintenanceRule kmRule(Long id, String type) {
        MaintenanceRule rule = new MaintenanceRule();
        rule.setId(id);
        rule.setCompanyId(COMPANY_ID);
        rule.setMaintenanceType(type);
        rule.setTriggerMode(MaintenanceDueCalculator.MODE_KM);
        rule.setIntervalKm(new BigDecimal("10000"));
        rule.setDueSoonKm(new BigDecimal("1000"));
        rule.setStatus("ACTIVE");
        return rule;
    }

    private static Vehicle vehicle(Long id, String code, String name, BigDecimal km, Long branchId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setCode(code);
        vehicle.setName(name);
        vehicle.setCompanyId(COMPANY_ID);
        vehicle.setBranchId(branchId);
        vehicle.setCurrentOdometerKm(km);
        vehicle.setIsDeleted(false);
        return vehicle;
    }

    private static VehicleMaintenanceBaseline baseline(MaintenanceRule rule, Vehicle vehicle, BigDecimal lastKm) {
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setRule(rule);
        baseline.setVehicle(vehicle);
        baseline.setLastServiceKm(lastKm);
        baseline.setCompanyId(COMPANY_ID);
        return baseline;
    }

    private static AppUser user(String username, Long companyId, Long branchId) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername(username);
        user.setCompanyId(companyId);
        user.setBranchId(branchId);
        return user;
    }
}
