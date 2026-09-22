package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceDueResponse;
import com.transport.erp.dto.WorkOrderCancelRequest;
import com.transport.erp.dto.WorkOrderCompleteRequest;
import com.transport.erp.dto.WorkOrderCreateRequest;
import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.dto.WorkOrderUpdateRequest;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.LookupValue;
import com.transport.erp.model.MaintenanceRule;
import com.transport.erp.model.Supplier;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleMaintenanceBaseline;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.LookupValueRepository;
import com.transport.erp.repository.MaintenanceRuleRepository;
import com.transport.erp.repository.SupplierRepository;
import com.transport.erp.repository.VehicleMaintenanceBaselineRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    static final String SOURCE_PREVENTIVE = "PREVENTIVE";
    static final String SOURCE_MANUAL = "MANUAL";
    static final String STATUS_OPEN = "OPEN";
    static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    static final String STATUS_COMPLETED = "COMPLETED";
    static final String STATUS_CANCELLED = "CANCELLED";
    static final String PRIORITY_LOW = "LOW";
    static final String PRIORITY_NORMAL = "NORMAL";
    static final String PRIORITY_HIGH = "HIGH";

    private static final String LOOKUP_TYPE = "MAINTENANCE_TYPE";
    private static final String PREVENTIVE_INDEX = "uk_work_orders_preventive_cycle";
    private static final Set<String> WRITE_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");
    private static final Set<String> SOURCES = Set.of(SOURCE_PREVENTIVE, SOURCE_MANUAL);
    private static final Set<String> PRIORITIES = Set.of(PRIORITY_LOW, PRIORITY_NORMAL, PRIORITY_HIGH);
    private static final Set<String> ACTIONABLE_DUE = Set.of(
            MaintenanceDueCalculator.DUE,
            MaintenanceDueCalculator.DUE_SOON,
            MaintenanceDueCalculator.OVERDUE);

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceRuleRepository ruleRepository;

    @Autowired
    private VehicleMaintenanceBaselineRepository baselineRepository;

    @Autowired
    private LookupValueRepository lookupValueRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> list(
            Long vehicleId,
            String status,
            String source,
            String maintenanceType,
            Long requestedBranchId,
            Long requestedCompanyId,
            Pageable pageable) {
        AppUser user = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return Page.empty(pageable);
        }
        Long branchFilter = user.getBranchId() != null ? user.getBranchId() : requestedBranchId;
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "openedAt"));
        Page<Long> ids = workOrderRepository.searchIds(
                companyId,
                vehicleId,
                normalizeFilter(status),
                normalizeFilter(source),
                normalizeFilter(maintenanceType),
                branchFilter,
                sorted);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), sorted, ids.getTotalElements());
        }
        Map<Long, WorkOrder> byId = workOrderRepository.findDetailsByIds(ids.getContent()).stream()
                .collect(Collectors.toMap(WorkOrder::getId, Function.identity(), (a, b) -> a));
        List<WorkOrderResponse> content = ids.getContent().stream()
                .map(byId::get)
                .filter(order -> order != null)
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(content, sorted, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse get(Long id) {
        return toResponse(requireReadable(id));
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderCreateRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        if (request == null || request.getVehicleId() == null) {
            throw vehicleInvalid();
        }

        Vehicle vehicle = lockAuthorizedVehicle(request.getVehicleId(), user);
        String source = requireSource(request.getSource());
        String priority = requirePriority(request.getPriority());
        String name = requireName(request.getName());

        WorkOrder order = new WorkOrder();
        order.setCompanyId(vehicle.getCompanyId());
        order.setBranchId(vehicle.getBranchId());
        order.setVehicle(vehicle);
        order.setSource(source);
        order.setPriority(priority);
        order.setName(name);
        order.setDescription(trimToNull(request.getDescription()));
        order.setStatus(STATUS_OPEN);
        order.setOpenedAt(LocalDateTime.now());
        order.setRequestedDate(request.getRequestedDate());
        order.setEstimatedCost(nonNegativeCost(request.getEstimatedCost(), "INVALID_ESTIMATED_COST", "Estimated cost cannot be negative."));
        order.setOdometerAtOpen(vehicle.getCurrentOdometerKm());
        order.setAttachmentPath(trimToNull(request.getAttachmentPath()));
        order.setCreatedBy(username);
        order.setUpdatedBy(username);
        order.setIsDeleted(false);

        if (SOURCE_PREVENTIVE.equals(source)) {
            applyPreventiveSnapshot(order, vehicle, request);
        } else {
            applyManual(order, vehicle, request);
        }

        order.setSupplier(resolveSupplier(request.getSupplierId(), vehicle.getCompanyId()));
        order.setAssignedUser(resolveAssignee(request.getAssignedUserId(), vehicle.getCompanyId()));

        order.setCode("TMP");
        order.setWorkOrderNumber("TMP-" + UUID.randomUUID());
        WorkOrder saved = saveNew(order);
        String number = "WO-" + String.format("%06d", saved.getId());
        saved.setCode(number);
        saved.setWorkOrderNumber(number);
        saved = workOrderRepository.save(saved);

        auditService.log(username, "WORK_ORDER_CREATED", "work_orders", saved.getId(), null,
                "number=" + number + ", vehicleId=" + vehicle.getId() + ", source=" + source);
        return toResponse(reload(saved.getId()));
    }

    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderUpdateRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockReadable(id, user);
        if (request == null) {
            throw notEditable("Completed and cancelled work orders cannot be edited.");
        }
        rejectImmutableChanges(request);
        String status = order.getStatus();
        if (STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status)) {
            throw notEditable("Completed and cancelled work orders cannot be edited.");
        }
        if (STATUS_IN_PROGRESS.equals(status)) {
            applyInProgressUpdate(order, request);
        } else if (STATUS_OPEN.equals(status)) {
            applyOpenUpdate(order, request);
        } else {
            throw notEditable("This work order cannot be edited.");
        }
        order.setUpdatedBy(username);
        workOrderRepository.save(order);
        auditService.log(username, "WORK_ORDER_UPDATED", "work_orders", order.getId(), null,
                "number=" + order.getWorkOrderNumber() + ", status=" + order.getStatus());
        return toResponse(reload(order.getId()));
    }

    @Transactional
    public WorkOrderResponse start(Long id, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockReadable(id, user);
        if (!STATUS_OPEN.equals(order.getStatus())) {
            throw invalidTransition("Only an open work order can be started.");
        }
        order.setStatus(STATUS_IN_PROGRESS);
        order.setStartedAt(LocalDateTime.now());
        order.setUpdatedBy(username);
        workOrderRepository.save(order);
        auditService.log(username, "WORK_ORDER_STARTED", "work_orders", order.getId(), null,
                "number=" + order.getWorkOrderNumber());
        return toResponse(reload(order.getId()));
    }

    @Transactional
    public WorkOrderResponse complete(Long id, WorkOrderCompleteRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        String notes = request != null ? trimToNull(request.getCompletionNotes()) : null;
        if (notes == null) {
            throw failure(
                    "Completion Notes Required",
                    "WORK_ORDER_COMPLETION_NOTES_REQUIRED",
                    "Completion notes are required.",
                    "Describe the work that was completed.");
        }
        BigDecimal actualCost = request.getActualCost() == null
                ? null
                : nonNegativeCost(request.getActualCost(), "INVALID_ACTUAL_COST", "Actual cost cannot be negative.");

        WorkOrder order = lockReadable(id, user);
        if (!STATUS_IN_PROGRESS.equals(order.getStatus())) {
            throw invalidTransition("Only an in-progress work order can be completed.");
        }
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(order.getVehicle().getId())
                .orElseThrow(this::vehicleInvalid);
        entityManager.refresh(vehicle);

        order.setStatus(STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setCompletedBy(username);
        order.setCompletionNotes(notes);
        order.setActualCost(actualCost);
        order.setOdometerAtComplete(vehicle.getCurrentOdometerKm());
        order.setUpdatedBy(username);
        workOrderRepository.save(order);
        auditService.log(username, "WORK_ORDER_COMPLETED", "work_orders", order.getId(), null,
                "number=" + order.getWorkOrderNumber() + ", odometerAtComplete=" + order.getOdometerAtComplete());
        return toResponse(reload(order.getId()));
    }

    @Transactional
    public WorkOrderResponse cancel(Long id, WorkOrderCancelRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        String reason = request != null ? trimToNull(request.getCancellationReason()) : null;
        if (reason == null) {
            throw failure(
                    "Cancellation Reason Required",
                    "WORK_ORDER_CANCEL_REASON_REQUIRED",
                    "A cancellation reason is required.",
                    "Enter why this work order is being cancelled.");
        }
        WorkOrder order = lockReadable(id, user);
        if (STATUS_COMPLETED.equals(order.getStatus())) {
            throw invalidTransition("Completed work orders cannot be cancelled.");
        }
        if (!STATUS_OPEN.equals(order.getStatus()) && !STATUS_IN_PROGRESS.equals(order.getStatus())) {
            throw invalidTransition("Only an open or in-progress work order can be cancelled.");
        }
        order.setStatus(STATUS_CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(username);
        order.setCancellationReason(reason);
        order.setUpdatedBy(username);
        workOrderRepository.save(order);
        auditService.log(username, "WORK_ORDER_CANCELLED", "work_orders", order.getId(), null,
                "number=" + order.getWorkOrderNumber());
        return toResponse(reload(order.getId()));
    }

    private void applyPreventiveSnapshot(WorkOrder order, Vehicle vehicle, WorkOrderCreateRequest request) {
        if (request.getMaintenanceRuleId() == null) {
            throw ruleInvalid();
        }
        MaintenanceRule rule = ruleRepository.findByIdAndIsDeletedFalse(request.getMaintenanceRuleId())
                .orElseThrow(this::ruleInvalid);
        if (!vehicle.getCompanyId().equals(rule.getCompanyId()) || !"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
            throw ruleInvalid();
        }
        tenantAccess.assertOwned(rule.getCompanyId());

        VehicleMaintenanceBaseline baseline = baselineRepository
                .findByRule_IdAndVehicle_IdAndIsDeletedFalse(rule.getId(), vehicle.getId())
                .orElse(null);
        MaintenanceDueResponse due = MaintenanceDueCalculator.calculate(rule, vehicle, baseline, LocalDate.now());
        if (!ACTIONABLE_DUE.contains(due.getDueStatus())) {
            throw failure(
                    "Work Order Not Due",
                    "WORK_ORDER_NOT_DUE",
                    "A preventive work order can only be opened when the rule is due, due soon, or overdue.",
                    "Wait until the maintenance rule is due, or create a manual work order.");
        }

        BigDecimal baselineKm = baseline != null ? baseline.getLastServiceKm() : null;
        LocalDate baselineDate = baseline != null ? baseline.getLastServiceDate() : null;
        long existing = workOrderRepository.countPreventiveCycle(
                vehicle.getCompanyId(), vehicle.getId(), rule.getId(), baselineKm, baselineDate);
        if (existing > 0) {
            throw duplicateOpen();
        }

        order.setMaintenanceRule(rule);
        order.setMaintenanceType(rule.getMaintenanceType());
        order.setTriggerMode(rule.getTriggerMode());
        order.setDueStatusAtCreation(due.getDueStatus());
        order.setDueKm(due.getNextDueKm());
        order.setDueDate(due.getNextDueDate());
        order.setBaselineLastServiceKm(baselineKm);
        order.setBaselineLastServiceDate(baselineDate);
    }

    private void applyManual(WorkOrder order, Vehicle vehicle, WorkOrderCreateRequest request) {
        if (request.getMaintenanceRuleId() != null) {
            throw ruleInvalid();
        }
        order.setMaintenanceRule(null);
        order.setMaintenanceType(requireMaintenanceType(vehicle.getCompanyId(), request.getMaintenanceType()));
        order.setTriggerMode(null);
        order.setDueStatusAtCreation(null);
        order.setDueKm(null);
        order.setDueDate(null);
        order.setBaselineLastServiceKm(null);
        order.setBaselineLastServiceDate(null);
    }

    private void applyOpenUpdate(WorkOrder order, WorkOrderUpdateRequest request) {
        boolean changed = false;
        if (request.getName() != null) {
            order.setName(requireName(request.getName()));
            changed = true;
        }
        if (request.getDescription() != null) {
            order.setDescription(trimToNull(request.getDescription()));
            changed = true;
        }
        if (request.getPriority() != null) {
            order.setPriority(requirePriority(request.getPriority()));
            changed = true;
        }
        if (request.getSupplierId() != null) {
            order.setSupplier(resolveSupplier(request.getSupplierId(), order.getCompanyId()));
            changed = true;
        }
        if (request.getAssignedUserId() != null) {
            order.setAssignedUser(resolveAssignee(request.getAssignedUserId(), order.getCompanyId()));
            changed = true;
        }
        if (request.getEstimatedCost() != null) {
            order.setEstimatedCost(nonNegativeCost(request.getEstimatedCost(), "INVALID_ESTIMATED_COST", "Estimated cost cannot be negative."));
            changed = true;
        }
        if (request.getRequestedDate() != null) {
            order.setRequestedDate(request.getRequestedDate());
            changed = true;
        }
        if (request.getDiagnosis() != null) {
            order.setDiagnosis(trimToNull(request.getDiagnosis()));
            changed = true;
        }
        if (request.getAttachmentPath() != null) {
            order.setAttachmentPath(trimToNull(request.getAttachmentPath()));
            changed = true;
        }
        if (!changed) {
            throw notEditable("No editable fields were provided.");
        }
    }

    private void applyInProgressUpdate(WorkOrder order, WorkOrderUpdateRequest request) {
        if (request.getName() != null
                || request.getDescription() != null
                || request.getPriority() != null
                || request.getSupplierId() != null
                || request.getAssignedUserId() != null
                || request.getRequestedDate() != null
                || request.getAttachmentPath() != null) {
            throw notEditable("In-progress work orders only allow diagnosis and estimated cost.");
        }
        boolean changed = false;
        if (request.getDiagnosis() != null) {
            order.setDiagnosis(trimToNull(request.getDiagnosis()));
            changed = true;
        }
        if (request.getEstimatedCost() != null) {
            order.setEstimatedCost(nonNegativeCost(request.getEstimatedCost(), "INVALID_ESTIMATED_COST", "Estimated cost cannot be negative."));
            changed = true;
        }
        if (!changed) {
            throw notEditable("In-progress work orders only allow diagnosis and estimated cost.");
        }
    }

    private void rejectImmutableChanges(WorkOrderUpdateRequest request) {
        if (request.getVehicleId() != null
                || request.getSource() != null
                || request.getMaintenanceRuleId() != null
                || request.getMaintenanceType() != null
                || request.getStatus() != null
                || request.getCompanyId() != null
                || request.getBranchId() != null
                || request.getOdometerAtOpen() != null
                || request.getOdometerAtComplete() != null
                || request.getActualCost() != null
                || request.getCompletionNotes() != null
                || request.getDueStatusAtCreation() != null
                || request.getDueKm() != null
                || request.getDueDate() != null
                || request.getBaselineLastServiceKm() != null
                || request.getBaselineLastServiceDate() != null) {
            throw notEditable("Vehicle, source, maintenance snapshot, odometer, company, and lifecycle fields cannot be changed.");
        }
    }

    private Vehicle lockAuthorizedVehicle(Long vehicleId, AppUser user) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(this::vehicleInvalid);
        entityManager.refresh(vehicle);
        try {
            parentAccess.requireVehicle(vehicleId);
        } catch (IllegalArgumentException ex) {
            throw vehicleInvalid();
        }
        tenantAccess.assertCompanyAccess(vehicle.getCompanyId());
        assertVehicleBranchAccess(vehicle, user);
        return vehicle;
    }

    private WorkOrder requireReadable(Long id) {
        AppUser user = tenantAccess.requireCurrentUser();
        WorkOrder order = workOrderRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + id));
        assertOrderAccess(order, user);
        return order;
    }

    private WorkOrder lockReadable(Long id, AppUser user) {
        WorkOrder order = workOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + id));
        assertOrderAccess(order, user);
        return order;
    }

    private WorkOrder reload(Long id) {
        return workOrderRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + id));
    }

    private void assertOrderAccess(WorkOrder order, AppUser user) {
        tenantAccess.assertCompanyAccess(order.getCompanyId());
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getBranchId() != null && order.getBranchId() != null
                && !user.getBranchId().equals(order.getBranchId())) {
            throw new AccessDeniedException("Access denied: Work order belongs to another branch.");
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

    private void assertWriteAccess(AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getRoles() == null || user.getRoles().stream().map(AppRole::getCode).noneMatch(WRITE_ROLES::contains)) {
            throw new AccessDeniedException("Access denied: work order updates require COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    private Supplier resolveSupplier(Long supplierId, Long companyId) {
        if (supplierId == null) {
            return null;
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + supplierId));
        if (!companyId.equals(supplier.getCompanyId())) {
            throw new AccessDeniedException("Access denied to another company's data");
        }
        tenantAccess.assertOwned(supplier.getCompanyId());
        return supplier;
    }

    private AppUser resolveAssignee(Long userId, Long companyId) {
        if (userId == null) {
            return null;
        }
        AppUser assignee = userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (!companyId.equals(assignee.getCompanyId())) {
            throw new AccessDeniedException("Access denied to another company's data");
        }
        tenantAccess.assertOwned(assignee.getCompanyId());
        return assignee;
    }

    private String requireMaintenanceType(Long companyId, String raw) {
        String type = raw != null ? raw.trim().toUpperCase(Locale.ROOT) : "";
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

    private WorkOrder saveNew(WorkOrder order) {
        try {
            return workOrderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException ex) {
            if (isPreventiveDuplicate(ex)) {
                throw duplicateOpen();
            }
            throw ex;
        }
    }

    private boolean isPreventiveDuplicate(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.contains(PREVENTIVE_INDEX);
    }

    private String requireSource(String raw) {
        String source = raw != null ? raw.trim().toUpperCase(Locale.ROOT) : "";
        if (!SOURCES.contains(source)) {
            throw failure(
                    "Invalid Work Order Source",
                    "WORK_ORDER_SOURCE_INVALID",
                    "Source must be PREVENTIVE or MANUAL.",
                    "Select preventive or manual.");
        }
        return source;
    }

    private String requirePriority(String raw) {
        String priority = raw == null || raw.isBlank()
                ? PRIORITY_NORMAL
                : raw.trim().toUpperCase(Locale.ROOT);
        if (!PRIORITIES.contains(priority)) {
            throw failure(
                    "Invalid Priority",
                    "WORK_ORDER_PRIORITY_INVALID",
                    "Priority must be LOW, NORMAL, or HIGH.",
                    "Select a valid priority.");
        }
        return priority;
    }

    private String requireName(String raw) {
        String name = raw != null ? raw.trim() : "";
        if (name.isEmpty() || name.length() > 150) {
            throw failure(
                    "Work Order Name Required",
                    "WORK_ORDER_NAME_REQUIRED",
                    "Work order name is required and must be 150 characters or fewer.",
                    "Enter a work order title.");
        }
        return name;
    }

    private BigDecimal nonNegativeCost(BigDecimal cost, String code, String message) {
        if (cost != null && cost.compareTo(BigDecimal.ZERO) < 0) {
            throw failure("Invalid Cost", code, message, "Enter zero or a positive amount.");
        }
        return cost;
    }

    private String normalizeFilter(String raw) {
        String value = trimToNull(raw);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private WorkOrderResponse toResponse(WorkOrder order) {
        WorkOrderResponse dto = new WorkOrderResponse();
        dto.setId(order.getId());
        dto.setCode(order.getCode());
        dto.setWorkOrderNumber(order.getWorkOrderNumber());
        dto.setName(order.getName());
        dto.setDescription(order.getDescription());
        dto.setStatus(order.getStatus());
        dto.setCompanyId(order.getCompanyId());
        dto.setBranchId(order.getBranchId());
        dto.setSource(order.getSource());
        if (order.getVehicle() != null) {
            dto.setVehicleId(order.getVehicle().getId());
            dto.setVehicleCode(order.getVehicle().getCode());
            dto.setVehicleName(order.getVehicle().getName());
        }
        if (order.getMaintenanceRule() != null) {
            dto.setMaintenanceRuleId(order.getMaintenanceRule().getId());
            dto.setMaintenanceRuleName(order.getMaintenanceRule().getName());
        }
        dto.setMaintenanceType(order.getMaintenanceType());
        dto.setTriggerMode(order.getTriggerMode());
        dto.setDueStatusAtCreation(order.getDueStatusAtCreation());
        dto.setDueKm(order.getDueKm());
        dto.setDueDate(order.getDueDate());
        dto.setBaselineLastServiceKm(order.getBaselineLastServiceKm());
        dto.setBaselineLastServiceDate(order.getBaselineLastServiceDate());
        dto.setPriority(order.getPriority());
        dto.setOpenedAt(order.getOpenedAt());
        dto.setStartedAt(order.getStartedAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setRequestedDate(order.getRequestedDate());
        dto.setOdometerAtOpen(order.getOdometerAtOpen());
        dto.setOdometerAtComplete(order.getOdometerAtComplete());
        if (order.getSupplier() != null) {
            dto.setSupplierId(order.getSupplier().getId());
            dto.setSupplierName(order.getSupplier().getName());
        }
        if (order.getAssignedUser() != null) {
            dto.setAssignedUserId(order.getAssignedUser().getId());
            dto.setAssignedUserName(order.getAssignedUser().getName() != null
                    ? order.getAssignedUser().getName()
                    : order.getAssignedUser().getUsername());
        }
        dto.setDiagnosis(order.getDiagnosis());
        dto.setCompletionNotes(order.getCompletionNotes());
        dto.setCancellationReason(order.getCancellationReason());
        dto.setEstimatedCost(order.getEstimatedCost());
        dto.setActualCost(order.getActualCost());
        dto.setAttachmentPath(order.getAttachmentPath());
        dto.setCreatedBy(order.getCreatedBy());
        dto.setCompletedBy(order.getCompletedBy());
        dto.setCancelledBy(order.getCancelledBy());
        dto.setVersion(order.getVersion());
        return dto;
    }

    private BusinessValidationException vehicleInvalid() {
        return failure(
                "Invalid Vehicle",
                "WORK_ORDER_VEHICLE_INVALID",
                "The vehicle is missing or is not available for a work order.",
                "Select an active vehicle in your company.");
    }

    private BusinessValidationException ruleInvalid() {
        return failure(
                "Invalid Maintenance Rule",
                "WORK_ORDER_RULE_INVALID",
                "Preventive work orders require an active maintenance rule for the vehicle company. Manual work orders cannot reference a rule.",
                "Select an active rule for this company, or create a manual work order.");
    }

    private BusinessValidationException duplicateOpen() {
        return failure(
                "Duplicate Preventive Work Order",
                "WORK_ORDER_DUPLICATE_OPEN",
                "A non-cancelled preventive work order already exists for this vehicle, rule, and service baseline.",
                "Open the existing work order, or cancel it before creating another for the same service cycle.");
    }

    private BusinessValidationException invalidTransition(String message) {
        return failure(
                "Invalid Work Order Transition",
                "WORK_ORDER_INVALID_TRANSITION",
                message,
                "Refresh the work order and use the action allowed for its current status.");
    }

    private BusinessValidationException notEditable(String message) {
        return failure(
                "Work Order Not Editable",
                "WORK_ORDER_NOT_EDITABLE",
                message,
                "Change only the fields allowed for the current status.");
    }

    private BusinessValidationException invalidMaintenanceType() {
        return failure(
                "Invalid Maintenance Type",
                "INVALID_MAINTENANCE_TYPE",
                "Maintenance type must be an existing MAINTENANCE_TYPE lookup value.",
                "Select a valid maintenance type from the lookup list.");
    }

    private BusinessValidationException failure(String title, String code, String message, String action) {
        return new BusinessValidationException(title, code, code + ": " + message, action);
    }
}
