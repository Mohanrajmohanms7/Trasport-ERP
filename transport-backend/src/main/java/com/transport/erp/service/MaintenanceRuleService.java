package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceBaselineRequest;
import com.transport.erp.dto.MaintenanceBaselineResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MaintenanceRuleService {

    static final BigDecimal DEFAULT_DUE_SOON_KM = new BigDecimal("1000");
    static final int DEFAULT_DUE_SOON_DAYS = 14;
    private static final String LOOKUP_TYPE = "MAINTENANCE_TYPE";
    private static final Set<String> WRITE_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");
    private static final Set<String> TRIGGER_MODES = Set.of(
            MaintenanceDueCalculator.MODE_KM,
            MaintenanceDueCalculator.MODE_DAYS,
            MaintenanceDueCalculator.MODE_KM_OR_DAYS);

    @Autowired
    private MaintenanceRuleRepository ruleRepository;

    @Autowired
    private VehicleMaintenanceBaselineRepository baselineRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private LookupValueRepository lookupValueRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private AuditService auditService;

    @Transactional(readOnly = true)
    public Page<MaintenanceRuleResponse> listRules(Long requestedCompanyId, Pageable pageable) {
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return Page.empty(pageable);
        }
        return ruleRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable).map(this::toRuleResponse);
    }

    @Transactional(readOnly = true)
    public MaintenanceRuleResponse getRule(Long id) {
        return toRuleResponse(requireOwnedRule(id));
    }

    @Transactional
    public MaintenanceRuleResponse createRule(MaintenanceRuleRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        Long companyId = requireTargetCompanyId(request != null ? request.getCompanyId() : null);

        String type = requireMaintenanceType(companyId, request);
        String mode = requireTriggerMode(request);
        applyIntervalValidation(mode, request);
        applyDueSoonDefaults(mode, request);

        assertNoDuplicateActive(companyId, type, null);

        MaintenanceRule rule = new MaintenanceRule();
        rule.setCode("MR-" + type + "-" + System.currentTimeMillis());
        rule.setName(requireName(request));
        rule.setDescription(trimToNull(request.getDescription()));
        rule.setStatus(normalizeStatus(request.getStatus(), "ACTIVE"));
        rule.setCompanyId(companyId);
        rule.setBranchId(user.getBranchId());
        rule.setMaintenanceType(type);
        rule.setTriggerMode(mode);
        rule.setIntervalKm(request.getIntervalKm());
        rule.setIntervalDays(request.getIntervalDays());
        rule.setDueSoonKm(request.getDueSoonKm());
        rule.setDueSoonDays(request.getDueSoonDays());
        rule.setCreatedBy(username);
        rule.setUpdatedBy(username);
        rule.setIsDeleted(false);

        MaintenanceRule saved = saveRule(rule);
        auditService.log(username, "MAINTENANCE_RULE_CREATED", "maintenance_rules", saved.getId(), null,
                String.format("ruleId=%d, maintenanceType=%s, triggerMode=%s", saved.getId(), type, mode));
        return toRuleResponse(saved);
    }

    @Transactional
    public MaintenanceRuleResponse updateRule(Long id, MaintenanceRuleRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        MaintenanceRule rule = requireOwnedRule(id);

        String type = requireMaintenanceType(rule.getCompanyId(), request);
        String mode = requireTriggerMode(request);
        applyIntervalValidation(mode, request);
        applyDueSoonDefaults(mode, request);

        if ("ACTIVE".equals(normalizeStatus(request.getStatus(), rule.getStatus()))) {
            assertNoDuplicateActive(rule.getCompanyId(), type, rule.getId());
        }

        String oldType = rule.getMaintenanceType();
        rule.setName(requireName(request));
        rule.setDescription(trimToNull(request.getDescription()));
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            rule.setStatus(normalizeStatus(request.getStatus(), rule.getStatus()));
        }
        rule.setMaintenanceType(type);
        rule.setTriggerMode(mode);
        rule.setIntervalKm(request.getIntervalKm());
        rule.setIntervalDays(request.getIntervalDays());
        rule.setDueSoonKm(request.getDueSoonKm());
        rule.setDueSoonDays(request.getDueSoonDays());
        rule.setUpdatedBy(username);

        MaintenanceRule saved = saveRule(rule);
        auditService.log(username, "MAINTENANCE_RULE_UPDATED", "maintenance_rules", saved.getId(), null,
                String.format("ruleId=%d, oldType=%s, newType=%s, triggerMode=%s, status=%s",
                        saved.getId(), oldType, type, mode, saved.getStatus()));
        return toRuleResponse(saved);
    }

    @Transactional
    public void deleteRule(Long id, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        MaintenanceRule rule = requireOwnedRule(id);
        rule.setIsDeleted(true);
        rule.setStatus("INACTIVE");
        rule.setUpdatedBy(username);
        ruleRepository.save(rule);
        auditService.log(username, "MAINTENANCE_RULE_DEACTIVATED", "maintenance_rules", rule.getId(), null,
                String.format("ruleId=%d, maintenanceType=%s", rule.getId(), rule.getMaintenanceType()));
    }

    @Transactional
    public MaintenanceBaselineResponse upsertBaseline(Long ruleId, Long vehicleId, MaintenanceBaselineRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);

        MaintenanceRule rule = requireOwnedRule(ruleId);
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
        if (Boolean.TRUE.equals(vehicle.getIsDeleted())) {
            throw new IllegalArgumentException("Vehicle not found: " + vehicleId);
        }
        tenantAccess.assertOwned(vehicle.getCompanyId());
        assertVehicleBranchAccess(vehicle, user);
        if (rule.getCompanyId() == null || !rule.getCompanyId().equals(vehicle.getCompanyId())) {
            throw new AccessDeniedException("Access denied to another company's data");
        }

        BigDecimal lastKm = request != null ? request.getLastServiceKm() : null;
        LocalDate lastDate = request != null ? request.getLastServiceDate() : null;
        validateBaseline(rule, lastKm, lastDate);

        VehicleMaintenanceBaseline baseline = baselineRepository
                .findByRule_IdAndVehicle_IdAndIsDeletedFalse(ruleId, vehicleId)
                .orElseGet(VehicleMaintenanceBaseline::new);

        boolean created = baseline.getId() == null;
        if (created) {
            baseline.setCode("BL-" + ruleId + "-" + vehicleId);
            baseline.setName(rule.getMaintenanceType() + " baseline");
            baseline.setStatus("ACTIVE");
            baseline.setCompanyId(vehicle.getCompanyId());
            baseline.setBranchId(vehicle.getBranchId());
            baseline.setRule(rule);
            baseline.setVehicle(vehicle);
            baseline.setCreatedBy(username);
            baseline.setIsDeleted(false);
        }
        BigDecimal oldKm = baseline.getLastServiceKm();
        LocalDate oldDate = baseline.getLastServiceDate();
        baseline.setLastServiceKm(lastKm);
        baseline.setLastServiceDate(lastDate);
        baseline.setUpdatedBy(username);

        VehicleMaintenanceBaseline saved;
        try {
            saved = baselineRepository.save(baseline);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateBaseline();
        }

        auditService.log(username, "MAINTENANCE_BASELINE_UPDATED", "vehicle_maintenance_baselines", saved.getId(), null,
                String.format("ruleId=%d, vehicleId=%d, maintenanceType=%s, oldKm=%s, newKm=%s, oldDate=%s, newDate=%s, created=%s",
                        ruleId, vehicleId, rule.getMaintenanceType(), oldKm, lastKm, oldDate, lastDate, created));
        return toBaselineResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceDueResponse> getDue(
            Long vehicleId,
            String maintenanceType,
            String dueStatus,
            Long requestedBranchId,
            Long requestedCompanyId,
            Pageable pageable,
            LocalDate today) {
        AppUser user = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return Page.empty(pageable);
        }
        LocalDate asOf = today != null ? today : LocalDate.now();

        String typeFilter = trimToNull(maintenanceType);
        if (typeFilter != null) {
            typeFilter = typeFilter.toUpperCase(Locale.ROOT);
        }
        String statusFilter = trimToNull(dueStatus);
        if (statusFilter != null) {
            statusFilter = statusFilter.toUpperCase(Locale.ROOT);
        }

        List<MaintenanceRule> rules = typeFilter == null
                ? ruleRepository.findByCompanyIdAndStatusAndIsDeletedFalse(companyId, "ACTIVE")
                : ruleRepository.findAllByCompanyIdAndMaintenanceTypeAndStatusAndIsDeletedFalse(companyId, typeFilter, "ACTIVE");
        if (rules.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Vehicle> vehicles = loadVehicles(companyId, vehicleId, requestedBranchId, user);
        if (vehicles.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> ruleIds = rules.stream().map(MaintenanceRule::getId).toList();
        Map<String, VehicleMaintenanceBaseline> baselines = baselineRepository
                .findByCompanyIdAndIsDeletedFalseAndRule_IdIn(companyId, ruleIds)
                .stream()
                .collect(Collectors.toMap(
                        b -> b.getRule().getId() + ":" + b.getVehicle().getId(),
                        Function.identity(),
                        (a, b) -> a));

        List<MaintenanceDueResponse> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            for (MaintenanceRule rule : rules) {
                VehicleMaintenanceBaseline baseline = baselines.get(rule.getId() + ":" + vehicle.getId());
                MaintenanceDueResponse row = MaintenanceDueCalculator.calculate(rule, vehicle, baseline, asOf);
                if (statusFilter == null || statusFilter.equals(row.getDueStatus())) {
                    rows.add(row);
                }
            }
        }

        int start = (int) pageable.getOffset();
        if (start >= rows.size()) {
            return new PageImpl<>(List.of(), pageable, rows.size());
        }
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }

    private List<Vehicle> loadVehicles(Long companyId, Long vehicleId, Long requestedBranchId, AppUser user) {
        if (vehicleId != null) {
            Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
            assertVehicleBranchAccess(vehicle, user);
            if (!companyId.equals(vehicle.getCompanyId()) && !tenantAccess.isSuperAdmin(user)) {
                throw new AccessDeniedException("Access denied to another company's data");
            }
            return List.of(vehicle);
        }
        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdAndIsDeletedFalseOrderByIdAsc(companyId);
        Long branchFilter = user.getBranchId() != null ? user.getBranchId() : requestedBranchId;
        if (branchFilter == null) {
            return vehicles;
        }
        return vehicles.stream()
                .filter(v -> v.getBranchId() == null || branchFilter.equals(v.getBranchId()))
                .collect(Collectors.toList());
    }

    private MaintenanceRule requireOwnedRule(Long id) {
        MaintenanceRule rule = ruleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance rule not found: " + id));
        tenantAccess.assertOwned(rule.getCompanyId());
        AppUser user = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(user)
                && user.getBranchId() != null && rule.getBranchId() != null
                && !user.getBranchId().equals(rule.getBranchId())) {
            throw new AccessDeniedException("Access denied: Rule belongs to another branch.");
        }
        return rule;
    }

    private Long requireTargetCompanyId(Long requestedCompanyId) {
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            throw new AccessDeniedException("User is not assigned to a company");
        }
        return companyId;
    }

    private void assertWriteAccess(AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getRoles() == null || user.getRoles().stream().map(AppRole::getCode).noneMatch(WRITE_ROLES::contains)) {
            throw new AccessDeniedException("Access denied: maintenance rule updates require COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    private void assertVehicleBranchAccess(Vehicle vehicle, AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getBranchId() != null && vehicle.getBranchId() != null
                && !user.getBranchId().equals(vehicle.getBranchId())) {
            throw new AccessDeniedException("Access denied: Vehicle belongs to another branch.");
        }
    }

    private String requireName(MaintenanceRuleRequest request) {
        if (request == null || trimToNull(request.getName()) == null) {
            throw new BusinessValidationException(
                    "Invalid Maintenance Rule",
                    "MAINTENANCE_RULE_NAME_REQUIRED",
                    "Maintenance rule name is required.",
                    "Provide a name for the maintenance rule."
            );
        }
        return request.getName().trim();
    }

    private String requireTriggerMode(MaintenanceRuleRequest request) {
        String mode = request != null && request.getTriggerMode() != null
                ? request.getTriggerMode().trim().toUpperCase(Locale.ROOT)
                : "";
        if (!TRIGGER_MODES.contains(mode)) {
            throw new BusinessValidationException(
                    "Invalid Trigger Mode",
                    "INVALID_TRIGGER_MODE",
                    "Trigger mode must be KM, DAYS, or KM_OR_DAYS.",
                    "Select a valid trigger mode."
            );
        }
        return mode;
    }

    private String requireMaintenanceType(Long companyId, MaintenanceRuleRequest request) {
        String type = request != null && request.getMaintenanceType() != null
                ? request.getMaintenanceType().trim().toUpperCase(Locale.ROOT)
                : "";
        if (type.isEmpty()) {
            throw invalidMaintenanceType();
        }
        Optional<LookupValue> lookup = lookupValueRepository
                .findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(companyId, LOOKUP_TYPE, type);
        if (lookup.isEmpty() || !"ACTIVE".equalsIgnoreCase(lookup.get().getStatus())) {
            throw invalidMaintenanceType();
        }
        return type;
    }

    private void applyIntervalValidation(String mode, MaintenanceRuleRequest request) {
        BigDecimal intervalKm = request.getIntervalKm();
        Integer intervalDays = request.getIntervalDays();
        if (isNegative(intervalKm) || (intervalDays != null && intervalDays < 0)) {
            throw invalidInterval();
        }
        boolean kmOk = isPositive(intervalKm);
        boolean daysOk = intervalDays != null && intervalDays > 0;
        if (MaintenanceDueCalculator.MODE_KM.equals(mode) && !kmOk) {
            throw invalidInterval();
        }
        if (MaintenanceDueCalculator.MODE_DAYS.equals(mode) && !daysOk) {
            throw invalidInterval();
        }
        if (MaintenanceDueCalculator.MODE_KM_OR_DAYS.equals(mode) && !kmOk && !daysOk) {
            throw invalidInterval();
        }
    }

    private void applyDueSoonDefaults(String mode, MaintenanceRuleRequest request) {
        if (isNegative(request.getDueSoonKm()) || (request.getDueSoonDays() != null && request.getDueSoonDays() < 0)) {
            throw new BusinessValidationException(
                    "Invalid Due-Soon Value",
                    "INVALID_DUE_SOON",
                    "Due-soon thresholds cannot be negative.",
                    "Enter zero or a positive due-soon threshold."
            );
        }
        boolean kmApplicable = MaintenanceDueCalculator.MODE_KM.equals(mode)
                || (MaintenanceDueCalculator.MODE_KM_OR_DAYS.equals(mode) && isPositive(request.getIntervalKm()));
        boolean daysApplicable = MaintenanceDueCalculator.MODE_DAYS.equals(mode)
                || (MaintenanceDueCalculator.MODE_KM_OR_DAYS.equals(mode) && request.getIntervalDays() != null && request.getIntervalDays() > 0);
        if (kmApplicable && request.getDueSoonKm() == null) {
            request.setDueSoonKm(DEFAULT_DUE_SOON_KM);
        }
        if (daysApplicable && request.getDueSoonDays() == null) {
            request.setDueSoonDays(DEFAULT_DUE_SOON_DAYS);
        }
    }

    private void validateBaseline(MaintenanceRule rule, BigDecimal lastKm, LocalDate lastDate) {
        if (lastKm != null && lastKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Invalid Baseline KM",
                    "INVALID_BASELINE_KM",
                    "Last service KM cannot be negative.",
                    "Enter a non-negative last service KM."
            );
        }
        String mode = rule.getTriggerMode();
        boolean kmNeeded = MaintenanceDueCalculator.MODE_KM.equals(mode)
                || (MaintenanceDueCalculator.MODE_KM_OR_DAYS.equals(mode) && isPositive(rule.getIntervalKm()));
        boolean dateNeeded = MaintenanceDueCalculator.MODE_DAYS.equals(mode)
                || (MaintenanceDueCalculator.MODE_KM_OR_DAYS.equals(mode) && rule.getIntervalDays() != null && rule.getIntervalDays() > 0);
        if (MaintenanceDueCalculator.MODE_KM.equals(mode) && lastKm == null) {
            throw baselineRequired("Last service KM is required for a KM maintenance rule.");
        }
        if (MaintenanceDueCalculator.MODE_DAYS.equals(mode) && lastDate == null) {
            throw baselineRequired("Last service date is required for a DAYS maintenance rule.");
        }
        if (MaintenanceDueCalculator.MODE_KM_OR_DAYS.equals(mode)) {
            boolean kmOk = !kmNeeded || lastKm != null;
            boolean dateOk = !dateNeeded || lastDate != null;
            if (kmNeeded && dateNeeded && lastKm == null && lastDate == null) {
                throw baselineRequired("Provide last service KM and/or last service date for a KM_OR_DAYS rule.");
            }
            if (kmNeeded && !dateNeeded && lastKm == null) {
                throw baselineRequired("Last service KM is required for this KM_OR_DAYS rule.");
            }
            if (dateNeeded && !kmNeeded && lastDate == null) {
                throw baselineRequired("Last service date is required for this KM_OR_DAYS rule.");
            }
            if (!kmOk && !dateOk) {
                throw baselineRequired("Provide at least one applicable baseline dimension.");
            }
        }
    }

    private void assertNoDuplicateActive(Long companyId, String type, Long excludeId) {
        Optional<MaintenanceRule> existing = ruleRepository
                .findByCompanyIdAndMaintenanceTypeAndStatusAndIsDeletedFalse(companyId, type, "ACTIVE");
        if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            throw duplicateRule();
        }
    }

    private MaintenanceRule saveRule(MaintenanceRule rule) {
        try {
            return ruleRepository.save(rule);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateRule();
        }
    }

    private BusinessValidationException duplicateRule() {
        return new BusinessValidationException(
                "Duplicate Maintenance Rule",
                "DUPLICATE_MAINTENANCE_RULE",
                "An active maintenance rule already exists for this company and maintenance type.",
                "Deactivate the existing rule or choose a different maintenance type."
        );
    }

    private BusinessValidationException duplicateBaseline() {
        return new BusinessValidationException(
                "Duplicate Baseline",
                "DUPLICATE_MAINTENANCE_BASELINE",
                "A baseline already exists for this rule and vehicle.",
                "Update the existing baseline instead of creating another."
        );
    }

    private BusinessValidationException invalidMaintenanceType() {
        return new BusinessValidationException(
                "Invalid Maintenance Type",
                "INVALID_MAINTENANCE_TYPE",
                "Maintenance type must be an existing MAINTENANCE_TYPE lookup value.",
                "Select a valid maintenance type from the lookup list."
        );
    }

    private BusinessValidationException invalidInterval() {
        return new BusinessValidationException(
                "Invalid Maintenance Interval",
                "INVALID_MAINTENANCE_INTERVAL",
                "A valid positive interval is required for the selected trigger mode.",
                "Provide intervalKm > 0 for KM, intervalDays > 0 for DAYS, or at least one positive interval for KM_OR_DAYS."
        );
    }

    private BusinessValidationException baselineRequired(String message) {
        return new BusinessValidationException(
                "Baseline Required",
                "MAINTENANCE_BASELINE_REQUIRED",
                message,
                "Enter the required last service KM and/or date for this rule."
        );
    }

    private MaintenanceRuleResponse toRuleResponse(MaintenanceRule rule) {
        MaintenanceRuleResponse dto = new MaintenanceRuleResponse();
        dto.setId(rule.getId());
        dto.setCode(rule.getCode());
        dto.setName(rule.getName());
        dto.setDescription(rule.getDescription());
        dto.setStatus(rule.getStatus());
        dto.setCompanyId(rule.getCompanyId());
        dto.setBranchId(rule.getBranchId());
        dto.setMaintenanceType(rule.getMaintenanceType());
        dto.setTriggerMode(rule.getTriggerMode());
        dto.setIntervalKm(rule.getIntervalKm());
        dto.setIntervalDays(rule.getIntervalDays());
        dto.setDueSoonKm(rule.getDueSoonKm());
        dto.setDueSoonDays(rule.getDueSoonDays());
        dto.setCreatedBy(rule.getCreatedBy());
        dto.setCreatedDate(rule.getCreatedDate());
        dto.setUpdatedBy(rule.getUpdatedBy());
        dto.setUpdatedDate(rule.getUpdatedDate());
        return dto;
    }

    private MaintenanceBaselineResponse toBaselineResponse(VehicleMaintenanceBaseline baseline) {
        MaintenanceBaselineResponse dto = new MaintenanceBaselineResponse();
        dto.setId(baseline.getId());
        dto.setRuleId(baseline.getRule().getId());
        dto.setVehicleId(baseline.getVehicle().getId());
        dto.setLastServiceKm(baseline.getLastServiceKm());
        dto.setLastServiceDate(baseline.getLastServiceDate());
        return dto;
    }

    private String normalizeStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback != null ? fallback : "ACTIVE";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new BusinessValidationException(
                    "Invalid Status",
                    "INVALID_MAINTENANCE_RULE_STATUS",
                    "Status must be ACTIVE or INACTIVE.",
                    "Set the rule status to ACTIVE or INACTIVE."
            );
        }
        return normalized;
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
