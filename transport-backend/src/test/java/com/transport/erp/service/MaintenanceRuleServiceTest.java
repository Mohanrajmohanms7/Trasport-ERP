package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceBaselineRequest;
import com.transport.erp.dto.MaintenanceDueResponse;
import com.transport.erp.dto.MaintenanceRuleRequest;
import com.transport.erp.dto.MaintenanceRuleResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.LookupValue;
import com.transport.erp.model.MaintenanceRule;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleMaintenanceBaseline;
import com.transport.erp.repository.LookupValueRepository;
import com.transport.erp.repository.MaintenanceRuleRepository;
import com.transport.erp.repository.VehicleMaintenanceBaselineRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintenanceRuleServiceTest {

    @Mock private MaintenanceRuleRepository ruleRepository;
    @Mock private VehicleMaintenanceBaselineRepository baselineRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private TenantAccessService tenantAccess;
    @Mock private TenantParentAccess parentAccess;
    @Mock private AuditService auditService;

    @InjectMocks
    private MaintenanceRuleService service;

    private AppUser manager;
    private Vehicle vehicle;
    private MaintenanceRule savedRule;

    @BeforeEach
    void setUp() {
        manager = userWithRole("manager", "COMPANY_ADMIN");
        when(tenantAccess.requireCurrentUser()).thenReturn(manager);
        when(tenantAccess.isSuperAdmin(any(AppUser.class))).thenReturn(false);
        when(tenantAccess.resolveCompanyId(null)).thenReturn(3L);
        doNothing().when(tenantAccess).assertOwned(3L);
        doThrow(new AccessDeniedException("Access denied to another company's data")).when(tenantAccess).assertOwned(99L);

        LookupValue oil = new LookupValue();
        oil.setType("MAINTENANCE_TYPE");
        oil.setCode("OIL_CHANGE");
        oil.setStatus("ACTIVE");
        when(lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(3L, "MAINTENANCE_TYPE", "OIL_CHANGE"))
                .thenReturn(Optional.of(oil));

        vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setCompanyId(3L);
        vehicle.setBranchId(1L);
        vehicle.setCurrentOdometerKm(new BigDecimal("49200"));
        vehicle.setIsDeleted(false);
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(parentAccess.requireVehicle(10L)).thenReturn(vehicle);
        when(vehicleRepository.findByCompanyIdAndIsDeletedFalseOrderByIdAsc(3L)).thenReturn(List.of(vehicle));

        when(ruleRepository.save(any(MaintenanceRule.class))).thenAnswer(inv -> {
            MaintenanceRule rule = inv.getArgument(0);
            if (rule.getId() == null) {
                rule.setId(50L);
            }
            savedRule = rule;
            return rule;
        });
        when(baselineRepository.save(any(VehicleMaintenanceBaseline.class))).thenAnswer(inv -> {
            VehicleMaintenanceBaseline baseline = inv.getArgument(0);
            if (baseline.getId() == null) {
                baseline.setId(70L);
            }
            return baseline;
        });
    }

    @Test
    @DisplayName("Valid KM rule is created with dueSoon default 1000")
    void createValidKmRule() {
        MaintenanceRuleResponse response = service.createRule(kmRequest(), "manager");
        assertEquals("OIL_CHANGE", response.getMaintenanceType());
        assertEquals("KM", response.getTriggerMode());
        assertEquals(0, new BigDecimal("1000").compareTo(response.getDueSoonKm()));
        verify(auditService).log(eq("manager"), eq("MAINTENANCE_RULE_CREATED"), eq("maintenance_rules"), eq(50L), isNull(), anyString());
    }

    @Test
    @DisplayName("Valid DAYS rule is created with dueSoonDays default 14")
    void createValidDaysRule() {
        MaintenanceRuleRequest req = new MaintenanceRuleRequest();
        req.setName("Engine date service");
        req.setMaintenanceType("OIL_CHANGE");
        req.setTriggerMode("DAYS");
        req.setIntervalDays(90);
        MaintenanceRuleResponse response = service.createRule(req, "manager");
        assertEquals("DAYS", response.getTriggerMode());
        assertEquals(14, response.getDueSoonDays());
        assertNull(response.getDueSoonKm());
    }

    @Test
    @DisplayName("Valid KM_OR_DAYS rule is created")
    void createValidKmOrDaysRule() {
        MaintenanceRuleRequest req = kmRequest();
        req.setTriggerMode("KM_OR_DAYS");
        req.setIntervalDays(60);
        MaintenanceRuleResponse response = service.createRule(req, "manager");
        assertEquals("KM_OR_DAYS", response.getTriggerMode());
        assertEquals(14, response.getDueSoonDays());
    }

    @Test
    @DisplayName("KM rule without interval is rejected")
    void missingKmInterval() {
        MaintenanceRuleRequest req = kmRequest();
        req.setIntervalKm(null);
        assertEquals("INVALID_MAINTENANCE_INTERVAL", errorCode(() -> service.createRule(req, "manager")));
    }

    @Test
    @DisplayName("DAYS rule without interval is rejected")
    void missingDaysInterval() {
        MaintenanceRuleRequest req = new MaintenanceRuleRequest();
        req.setName("Date rule");
        req.setMaintenanceType("OIL_CHANGE");
        req.setTriggerMode("DAYS");
        assertEquals("INVALID_MAINTENANCE_INTERVAL", errorCode(() -> service.createRule(req, "manager")));
    }

    @Test
    @DisplayName("KM_OR_DAYS with both intervals missing is rejected")
    void bothIntervalsMissing() {
        MaintenanceRuleRequest req = kmRequest();
        req.setTriggerMode("KM_OR_DAYS");
        req.setIntervalKm(null);
        req.setIntervalDays(null);
        assertEquals("INVALID_MAINTENANCE_INTERVAL", errorCode(() -> service.createRule(req, "manager")));
    }

    @Test
    @DisplayName("Zero interval is rejected")
    void zeroInterval() {
        MaintenanceRuleRequest req = kmRequest();
        req.setIntervalKm(BigDecimal.ZERO);
        assertEquals("INVALID_MAINTENANCE_INTERVAL", errorCode(() -> service.createRule(req, "manager")));
    }

    @Test
    @DisplayName("Negative interval is rejected")
    void negativeInterval() {
        MaintenanceRuleRequest req = kmRequest();
        req.setIntervalKm(new BigDecimal("-1"));
        assertEquals("INVALID_MAINTENANCE_INTERVAL", errorCode(() -> service.createRule(req, "manager")));
    }

    @Test
    @DisplayName("Negative due-soon is rejected")
    void negativeDueSoon() {
        MaintenanceRuleRequest req = kmRequest();
        req.setDueSoonKm(new BigDecimal("-5"));
        assertEquals("INVALID_DUE_SOON", errorCode(() -> service.createRule(req, "manager")));
    }

    @Test
    @DisplayName("Unknown maintenance type is rejected")
    void invalidMaintenanceType() {
        when(lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(3L, "MAINTENANCE_TYPE", "WAX"))
                .thenReturn(Optional.empty());
        MaintenanceRuleRequest req = kmRequest();
        req.setMaintenanceType("WAX");
        assertEquals("INVALID_MAINTENANCE_TYPE", errorCode(() -> service.createRule(req, "manager")));
        verify(lookupValueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate active rule is rejected")
    void duplicateActiveRule() {
        MaintenanceRule existing = new MaintenanceRule();
        existing.setId(9L);
        existing.setMaintenanceType("OIL_CHANGE");
        when(ruleRepository.findByCompanyIdAndMaintenanceTypeAndStatusAndIsDeletedFalse(3L, "OIL_CHANGE", "ACTIVE"))
                .thenReturn(Optional.of(existing));
        assertEquals("DUPLICATE_MAINTENANCE_RULE", errorCode(() -> service.createRule(kmRequest(), "manager")));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("KM baseline is saved without changing vehicle KM")
    void validKmBaseline() {
        MaintenanceRule rule = persistableRule();
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        when(baselineRepository.findByRule_IdAndVehicle_IdAndIsDeletedFalse(50L, 10L)).thenReturn(Optional.empty());

        MaintenanceBaselineRequest req = new MaintenanceBaselineRequest();
        req.setLastServiceKm(new BigDecimal("40000"));
        service.upsertBaseline(50L, 10L, req, "manager");

        assertEquals(0, new BigDecimal("49200").compareTo(vehicle.getCurrentOdometerKm()));
        verify(vehicleRepository, never()).save(any());
        verify(auditService).log(eq("manager"), eq("MAINTENANCE_BASELINE_UPDATED"), eq("vehicle_maintenance_baselines"), any(), isNull(), contains("vehicleId=10"));
    }

    @Test
    @DisplayName("KM rule without KM baseline is rejected")
    void kmBaselineMissingKm() {
        MaintenanceRule rule = persistableRule();
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        MaintenanceBaselineRequest req = new MaintenanceBaselineRequest();
        req.setLastServiceDate(LocalDate.of(2026, 9, 1));
        assertEquals("MAINTENANCE_BASELINE_REQUIRED", errorCode(() -> service.upsertBaseline(50L, 10L, req, "manager")));
        verify(baselineRepository, never()).save(any());
    }

    @Test
    @DisplayName("DAYS rule without date baseline is rejected")
    void daysBaselineMissingDate() {
        MaintenanceRule rule = persistableRule();
        rule.setTriggerMode("DAYS");
        rule.setIntervalKm(null);
        rule.setIntervalDays(90);
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        MaintenanceBaselineRequest req = new MaintenanceBaselineRequest();
        req.setLastServiceKm(new BigDecimal("40000"));
        assertEquals("MAINTENANCE_BASELINE_REQUIRED", errorCode(() -> service.upsertBaseline(50L, 10L, req, "manager")));
    }

    @Test
    @DisplayName("KM_OR_DAYS accepts one valid dimension")
    void kmOrDaysOneDimension() {
        MaintenanceRule rule = persistableRule();
        rule.setTriggerMode("KM_OR_DAYS");
        rule.setIntervalDays(60);
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        when(baselineRepository.findByRule_IdAndVehicle_IdAndIsDeletedFalse(50L, 10L)).thenReturn(Optional.empty());
        MaintenanceBaselineRequest req = new MaintenanceBaselineRequest();
        req.setLastServiceKm(new BigDecimal("40000"));
        assertNotNull(service.upsertBaseline(50L, 10L, req, "manager"));
    }

    @Test
    @DisplayName("Negative baseline KM is rejected")
    void negativeBaselineKm() {
        MaintenanceRule rule = persistableRule();
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        MaintenanceBaselineRequest req = new MaintenanceBaselineRequest();
        req.setLastServiceKm(new BigDecimal("-1"));
        assertEquals("INVALID_BASELINE_KM", errorCode(() -> service.upsertBaseline(50L, 10L, req, "manager")));
    }

    @Test
    @DisplayName("Cross-company vehicle is denied")
    void crossCompanyVehicle() {
        MaintenanceRule rule = persistableRule();
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        vehicle.setCompanyId(99L);
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        MaintenanceBaselineRequest req = new MaintenanceBaselineRequest();
        req.setLastServiceKm(new BigDecimal("40000"));
        assertThrows(AccessDeniedException.class, () -> service.upsertBaseline(50L, 10L, req, "manager"));
        verify(baselineRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cross-company rule is denied")
    void crossCompanyRule() {
        MaintenanceRule rule = persistableRule();
        rule.setCompanyId(99L);
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        assertThrows(AccessDeniedException.class, () -> service.getRule(50L));
    }

    @Test
    @DisplayName("Operator cannot write rules")
    void operatorCannotWrite() {
        when(tenantAccess.requireCurrentUser()).thenReturn(userWithRole("op", "OPERATOR"));
        assertThrows(AccessDeniedException.class, () -> service.createRule(kmRequest(), "op"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Accountant cannot write baselines")
    void accountantCannotWriteBaseline() {
        when(tenantAccess.requireCurrentUser()).thenReturn(userWithRole("acct", "ACCOUNTANT"));
        MaintenanceRule rule = persistableRule();
        when(ruleRepository.findByIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(rule));
        MaintenanceBaselineRequest req = new MaintenanceBaselineRequest();
        req.setLastServiceKm(new BigDecimal("40000"));
        assertThrows(AccessDeniedException.class, () -> service.upsertBaseline(50L, 10L, req, "acct"));
        verify(baselineRepository, never()).save(any());
    }

    @Test
    @DisplayName("Viewer cannot write")
    void viewerCannotWrite() {
        when(tenantAccess.requireCurrentUser()).thenReturn(userWithRole("view", "VIEWER"));
        assertThrows(AccessDeniedException.class, () -> service.deleteRule(50L, "view"));
        assertThrows(AccessDeniedException.class, () -> service.createRule(kmRequest(), "view"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("SUPER_ADMIN creates a rule for the resolved target company")
    void superAdminCreatesForTargetCompany() {
        AppUser superAdmin = userWithRole("admin", "SUPER_ADMIN");
        superAdmin.setCompanyId(null);
        superAdmin.setBranchId(null);
        when(tenantAccess.requireCurrentUser()).thenReturn(superAdmin);
        when(tenantAccess.isSuperAdmin(superAdmin)).thenReturn(true);
        when(tenantAccess.resolveCompanyId(3L)).thenReturn(3L);

        MaintenanceRuleRequest req = kmRequest();
        req.setCompanyId(3L);
        MaintenanceRuleResponse response = service.createRule(req, "admin");

        assertEquals(3L, response.getCompanyId());
        assertNotNull(response.getCompanyId());
        assertEquals("OIL_CHANGE", response.getMaintenanceType());
        assertEquals(3L, savedRule.getCompanyId());
        verify(lookupValueRepository).findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(3L, "MAINTENANCE_TYPE", "OIL_CHANGE");
        verify(tenantAccess).resolveCompanyId(3L);
    }

    @Test
    @DisplayName("SUPER_ADMIN maintenance type lookup uses the target company not a default")
    void superAdminLookupUsesTargetCompany() {
        AppUser superAdmin = userWithRole("admin", "SUPER_ADMIN");
        superAdmin.setCompanyId(null);
        when(tenantAccess.requireCurrentUser()).thenReturn(superAdmin);
        when(tenantAccess.isSuperAdmin(superAdmin)).thenReturn(true);
        when(tenantAccess.resolveCompanyId(4L)).thenReturn(4L);
        LookupValue oil = new LookupValue();
        oil.setType("MAINTENANCE_TYPE");
        oil.setCode("OIL_CHANGE");
        oil.setStatus("ACTIVE");
        when(lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(4L, "MAINTENANCE_TYPE", "OIL_CHANGE"))
                .thenReturn(Optional.of(oil));

        MaintenanceRuleRequest req = kmRequest();
        req.setCompanyId(4L);
        MaintenanceRuleResponse response = service.createRule(req, "admin");

        assertEquals(4L, response.getCompanyId());
        verify(lookupValueRepository).findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(4L, "MAINTENANCE_TYPE", "OIL_CHANGE");
        verify(lookupValueRepository, never()).findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(eq(3L), anyString(), anyString());
    }

    @Test
    @DisplayName("SUPER_ADMIN is denied when tenant access rejects the target company")
    void superAdminUnauthorizedCompanyDenied() {
        AppUser superAdmin = userWithRole("admin", "SUPER_ADMIN");
        superAdmin.setCompanyId(null);
        when(tenantAccess.requireCurrentUser()).thenReturn(superAdmin);
        when(tenantAccess.isSuperAdmin(superAdmin)).thenReturn(true);
        when(tenantAccess.resolveCompanyId(99L))
                .thenThrow(new AccessDeniedException("Access denied to another company's data"));

        MaintenanceRuleRequest req = kmRequest();
        req.setCompanyId(99L);
        assertThrows(AccessDeniedException.class, () -> service.createRule(req, "admin"));
        verify(ruleRepository, never()).save(any());
        verify(lookupValueRepository, never()).findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(eq(99L), anyString(), anyString());
    }

    @Test
    @DisplayName("SUPER_ADMIN without a resolved company cannot create a company-less rule")
    void superAdminMissingCompanyContextDenied() {
        AppUser superAdmin = userWithRole("admin", "SUPER_ADMIN");
        superAdmin.setCompanyId(null);
        when(tenantAccess.requireCurrentUser()).thenReturn(superAdmin);
        when(tenantAccess.isSuperAdmin(superAdmin)).thenReturn(true);
        when(tenantAccess.resolveCompanyId(null)).thenReturn(null);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> service.createRule(kmRequest(), "admin"));
        assertEquals("User is not assigned to a company", ex.getMessage());
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("COMPANY_ADMIN create still uses authenticated company and ignores client companyId")
    void companyAdminCreateIgnoresClientCompanyId() {
        MaintenanceRuleRequest req = kmRequest();
        req.setCompanyId(99L);
        when(tenantAccess.resolveCompanyId(99L)).thenReturn(3L);

        MaintenanceRuleResponse response = service.createRule(req, "manager");
        assertEquals(3L, response.getCompanyId());
        verify(lookupValueRepository).findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(3L, "MAINTENANCE_TYPE", "OIL_CHANGE");
        verify(lookupValueRepository, never()).findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(eq(99L), anyString(), anyString());
    }

    @Test
    @DisplayName("BRANCH_MANAGER can create a rule for the authenticated company")
    void branchManagerCanCreate() {
        AppUser branchManager = userWithRole("mgr", "BRANCH_MANAGER");
        when(tenantAccess.requireCurrentUser()).thenReturn(branchManager);
        when(tenantAccess.isSuperAdmin(branchManager)).thenReturn(false);
        when(tenantAccess.resolveCompanyId(null)).thenReturn(3L);

        MaintenanceRuleResponse response = service.createRule(kmRequest(), "mgr");
        assertEquals(3L, response.getCompanyId());
        assertEquals("OIL_CHANGE", response.getMaintenanceType());
    }

    @Test
    @DisplayName("Accountant cannot create rules")
    void accountantCannotCreate() {
        when(tenantAccess.requireCurrentUser()).thenReturn(userWithRole("acct", "ACCOUNTANT"));
        assertThrows(AccessDeniedException.class, () -> service.createRule(kmRequest(), "acct"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Due GET uses current vehicle KM so a correction changes status")
    void dueUsesCurrentOdometerAfterCorrection() {
        MaintenanceRule rule = persistableRule();
        rule.setId(50L);
        VehicleMaintenanceBaseline baseline = new VehicleMaintenanceBaseline();
        baseline.setRule(rule);
        baseline.setVehicle(vehicle);
        baseline.setLastServiceKm(new BigDecimal("40000"));
        when(ruleRepository.findByCompanyIdAndStatusAndIsDeletedFalse(3L, "ACTIVE")).thenReturn(List.of(rule));
        when(baselineRepository.findByCompanyIdAndIsDeletedFalseAndRule_IdIn(eq(3L), any())).thenReturn(List.of(baseline));

        Page<MaintenanceDueResponse> first = service.getDue(null, null, null, null, null, PageRequest.of(0, 20), LocalDate.of(2026, 9, 21));
        assertEquals(MaintenanceDueCalculator.DUE_SOON, first.getContent().get(0).getDueStatus());

        vehicle.setCurrentOdometerKm(new BigDecimal("50001"));
        Page<MaintenanceDueResponse> second = service.getDue(null, null, null, null, null, PageRequest.of(0, 20), LocalDate.of(2026, 9, 21));
        assertEquals(MaintenanceDueCalculator.OVERDUE, second.getContent().get(0).getDueStatus());
    }

    @Test
    @DisplayName("Inactive rules are excluded from due list")
    void inactiveRulesExcludedFromDue() {
        when(ruleRepository.findByCompanyIdAndStatusAndIsDeletedFalse(3L, "ACTIVE")).thenReturn(List.of());
        Page<MaintenanceDueResponse> page = service.getDue(null, null, null, null, null, PageRequest.of(0, 20), LocalDate.of(2026, 9, 21));
        assertTrue(page.isEmpty());
        verify(vehicleRepository, never()).findByCompanyIdAndIsDeletedFalseOrderByIdAsc(any());
    }

    private MaintenanceRule persistableRule() {
        MaintenanceRule rule = new MaintenanceRule();
        rule.setId(50L);
        rule.setName("Oil");
        rule.setCompanyId(3L);
        rule.setBranchId(1L);
        rule.setMaintenanceType("OIL_CHANGE");
        rule.setTriggerMode("KM");
        rule.setIntervalKm(new BigDecimal("10000"));
        rule.setDueSoonKm(new BigDecimal("1000"));
        rule.setStatus("ACTIVE");
        rule.setIsDeleted(false);
        return rule;
    }

    private MaintenanceRuleRequest kmRequest() {
        MaintenanceRuleRequest req = new MaintenanceRuleRequest();
        req.setName("Engine oil");
        req.setMaintenanceType("OIL_CHANGE");
        req.setTriggerMode("KM");
        req.setIntervalKm(new BigDecimal("10000"));
        return req;
    }

    private String errorCode(Runnable action) {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, action::run);
        return ex.getErrorCode();
    }

    private AppUser userWithRole(String username, String roleCode) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername(username);
        user.setCompanyId(3L);
        user.setBranchId(1L);
        AppRole role = new AppRole();
        role.setCode(roleCode);
        role.setName(roleCode);
        Set<AppRole> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }
}
