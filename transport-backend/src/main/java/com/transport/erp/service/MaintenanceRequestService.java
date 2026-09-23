package com.transport.erp.service;

import com.transport.erp.dto.AuthorizedVehicleResponse;
import com.transport.erp.dto.MaintenanceRequestCancelRequest;
import com.transport.erp.dto.MaintenanceRequestCreateRequest;
import com.transport.erp.dto.MaintenanceRequestResponse;
import com.transport.erp.dto.MaintenanceRequestReviewRequest;
import com.transport.erp.dto.MaintenanceRequestUpdateRequest;
import com.transport.erp.dto.WorkOrderCreateRequest;
import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Driver;
import com.transport.erp.model.MaintenanceRequest;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.MaintenanceRequestRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MaintenanceRequestService {

    static final String STATUS_OPEN = "OPEN";
    static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    static final String STATUS_APPROVED = "APPROVED";
    static final String STATUS_CONVERTED = "CONVERTED";
    static final String STATUS_CANCELLED = "CANCELLED";

    static final String PRIORITY_LOW = "LOW";
    static final String PRIORITY_MEDIUM = "MEDIUM";
    static final String PRIORITY_HIGH = "HIGH";
    static final String PRIORITY_CRITICAL = "CRITICAL";

    private static final String WORK_ORDER_TYPE = "GENERAL_SERVICE";
    private static final Set<String> ADMIN_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");
    private static final Set<String> PRIORITIES = Set.of(
            PRIORITY_LOW, PRIORITY_MEDIUM, PRIORITY_HIGH, PRIORITY_CRITICAL);
    @Autowired
    private MaintenanceRequestRepository requestRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private DriverVehicleAuthorizationService driverAuthorization;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponse> list(
            Long vehicleId,
            String status,
            String priority,
            Long requestedById,
            LocalDate fromDate,
            LocalDate toDate,
            Long requestedBranchId,
            Long requestedCompanyId,
            Pageable pageable) {
        AppUser user = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return Page.empty(pageable);
        }
        boolean driverOnly = isDriverOnly(user);
        Collection<Long> vehicleIds = List.of();
        if (driverOnly) {
            vehicleIds = driverAuthorization.authorizedVehicleIds(user);
            if (vehicleIds.isEmpty()) {
                return Page.empty(pageable);
            }
            if (vehicleId != null && !vehicleIds.contains(vehicleId)) {
                throw driverAuthorizationFailure();
            }
        }
        Long branchFilter = driverOnly
                ? null
                : (user.getBranchId() != null && !tenantAccess.isSuperAdmin(user)
                ? user.getBranchId()
                : requestedBranchId);
        if (tenantAccess.isSuperAdmin(user)) {
            branchFilter = requestedBranchId;
        }
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "requestedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        LocalDateTime fromAt = fromDate == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : fromDate.atStartOfDay();
        LocalDateTime toAt = toDate == null ? LocalDateTime.of(9999, 1, 1, 0, 0) : toDate.plusDays(1).atStartOfDay();
        Page<Long> ids = driverOnly
                ? requestRepository.searchIdsForVehicles(
                        companyId, vehicleIds, vehicleId, normalize(status), normalize(priority),
                        requestedById, fromAt, toAt, sorted)
                : requestRepository.searchIds(
                        companyId, vehicleId, normalize(status), normalize(priority),
                        requestedById, fromAt, toAt, branchFilter, sorted);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), sorted, ids.getTotalElements());
        }
        Map<Long, MaintenanceRequest> byId = requestRepository.findDetailsByIds(ids.getContent()).stream()
                .collect(Collectors.toMap(MaintenanceRequest::getId, Function.identity(), (a, b) -> a));
        List<MaintenanceRequestResponse> content = ids.getContent().stream()
                .map(byId::get)
                .filter(row -> row != null)
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(content, sorted, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public MaintenanceRequestResponse get(Long id) {
        return toResponse(requireReadable(id));
    }

    @Transactional(readOnly = true)
    public List<AuthorizedVehicleResponse> authorizedVehicles() {
        AppUser user = tenantAccess.requireCurrentUser();
        if (!isDriverOnly(user)) {
            throw new AccessDeniedException("Authorized vehicle list is for a driver login.");
        }
        return driverAuthorization.authorizedVehicles(user);
    }

    @Transactional
    public MaintenanceRequestResponse create(MaintenanceRequestCreateRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        if (request == null || request.getVehicleId() == null) {
            throw failure(
                    "Vehicle Required",
                    "MAINTENANCE_REQUEST_VEHICLE_REQUIRED",
                    "MAINTENANCE_REQUEST_VEHICLE_REQUIRED: A vehicle is required.",
                    "Select a vehicle.");
        }
        Vehicle vehicle = lockVehicle(request.getVehicleId(), user);
        Driver driver = null;
        if (isDriverOnly(user)) {
            driver = driverAuthorization.requireLinkedDriver(user);
            driverAuthorization.assertAuthorized(user, driver, vehicle);
        } else {
            assertAdmin(user);
        }
        String title = requireTitle(request.getTitle());
        String description = requireDescription(request.getDescription());
        String priority = requirePriority(request.getPriority());

        MaintenanceRequest row = new MaintenanceRequest();
        row.setCompanyId(vehicle.getCompanyId());
        row.setBranchId(vehicle.getBranchId());
        row.setVehicle(vehicle);
        row.setRequestedBy(user);
        row.setDriver(driver);
        row.setName(title);
        row.setDescription(description);
        row.setPriority(priority);
        row.setStatus(STATUS_OPEN);
        row.setRequestedAt(LocalDateTime.now());
        row.setReportedOdometerKm(scaleOdometer(vehicle.getCurrentOdometerKm()));
        row.setCreatedBy(username);
        row.setUpdatedBy(username);
        row.setIsDeleted(false);
        row.setCode("TMP");
        row.setRequestNumber("TMP-" + UUID.randomUUID());
        MaintenanceRequest saved = requestRepository.saveAndFlush(row);
        String number = "MR-" + String.format("%06d", saved.getId());
        saved.setCode(number);
        saved.setRequestNumber(number);
        saved = requestRepository.save(saved);
        auditService.log(username, "MAINTENANCE_REQUEST_CREATED", "maintenance_requests", saved.getId(), null,
                "number=" + number + ", vehicleId=" + vehicle.getId());
        return toResponse(reload(saved.getId()));
    }

    @Transactional
    public MaintenanceRequestResponse update(Long id, MaintenanceRequestUpdateRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        MaintenanceRequest row = lockReadable(id, user);
        if (!STATUS_OPEN.equals(row.getStatus())) {
            throw notEditable();
        }
        if (request == null) {
            throw notEditable();
        }
        boolean changed = false;
        if (request.getVehicleId() != null && !request.getVehicleId().equals(row.getVehicle().getId())) {
            Vehicle vehicle = lockVehicle(request.getVehicleId(), user);
            if (isDriverOnly(user)) {
                Driver driver = driverAuthorization.requireLinkedDriver(user);
                driverAuthorization.assertAuthorized(user, driver, vehicle);
                row.setDriver(driver);
            } else {
                assertAdmin(user);
            }
            row.setVehicle(vehicle);
            row.setCompanyId(vehicle.getCompanyId());
            row.setBranchId(vehicle.getBranchId());
            row.setReportedOdometerKm(scaleOdometer(vehicle.getCurrentOdometerKm()));
            changed = true;
        }
        if (request.getTitle() != null) {
            row.setName(requireTitle(request.getTitle()));
            changed = true;
        }
        if (request.getDescription() != null) {
            row.setDescription(requireDescription(request.getDescription()));
            changed = true;
        }
        if (request.getPriority() != null) {
            row.setPriority(requirePriority(request.getPriority()));
            changed = true;
        }
        if (!changed) {
            throw notEditable();
        }
        row.setUpdatedBy(username);
        requestRepository.save(row);
        auditService.log(username, "MAINTENANCE_REQUEST_UPDATED", "maintenance_requests", row.getId(), null,
                "number=" + row.getRequestNumber());
        return toResponse(reload(row.getId()));
    }

    @Transactional
    public MaintenanceRequestResponse review(Long id, MaintenanceRequestReviewRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertAdmin(user);
        MaintenanceRequest row = lockReadable(id, user);
        if (!STATUS_OPEN.equals(row.getStatus())) {
            throw invalidTransition("Only an open request can be reviewed.");
        }
        row.setStatus(STATUS_UNDER_REVIEW);
        row.setReviewedBy(username);
        row.setReviewedAt(LocalDateTime.now());
        row.setReviewRemarks(request == null ? null : trimToNull(request.getReviewRemarks()));
        row.setUpdatedBy(username);
        requestRepository.save(row);
        auditService.log(username, "MAINTENANCE_REQUEST_REVIEWED", "maintenance_requests", row.getId(), null,
                "number=" + row.getRequestNumber());
        return toResponse(reload(row.getId()));
    }

    @Transactional
    public MaintenanceRequestResponse approve(Long id, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertAdmin(user);
        MaintenanceRequest row = lockReadable(id, user);
        if (STATUS_CONVERTED.equals(row.getStatus())) {
            throw alreadyConverted();
        }
        if (!STATUS_UNDER_REVIEW.equals(row.getStatus())) {
            throw invalidTransition("Only a request under review can be approved.");
        }
        row.setStatus(STATUS_APPROVED);
        row.setApprovedBy(username);
        row.setApprovedAt(LocalDateTime.now());
        row.setUpdatedBy(username);
        requestRepository.save(row);
        auditService.log(username, "MAINTENANCE_REQUEST_APPROVED", "maintenance_requests", row.getId(), null,
                "number=" + row.getRequestNumber());
        return toResponse(reload(row.getId()));
    }

    @Transactional
    public MaintenanceRequestResponse cancel(Long id, MaintenanceRequestCancelRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertAdmin(user);
        String reason = request == null ? null : trimToNull(request.getCancellationReason());
        if (reason == null) {
            throw failure(
                    "Cancellation Reason Required",
                    "MAINTENANCE_REQUEST_INVALID_TRANSITION",
                    "MAINTENANCE_REQUEST_INVALID_TRANSITION: A cancellation reason is required.",
                    "Enter why this request is being cancelled.");
        }
        MaintenanceRequest row = lockReadable(id, user);
        if (STATUS_CONVERTED.equals(row.getStatus()) || row.getWorkOrder() != null) {
            throw alreadyConverted();
        }
        if (!STATUS_OPEN.equals(row.getStatus()) && !STATUS_UNDER_REVIEW.equals(row.getStatus())) {
            throw invalidTransition("Only an open or under-review request can be cancelled.");
        }
        row.setStatus(STATUS_CANCELLED);
        row.setCancelledBy(username);
        row.setCancelledAt(LocalDateTime.now());
        row.setCancellationReason(reason);
        row.setUpdatedBy(username);
        requestRepository.save(row);
        auditService.log(username, "MAINTENANCE_REQUEST_CANCELLED", "maintenance_requests", row.getId(), null,
                "number=" + row.getRequestNumber());
        return toResponse(reload(row.getId()));
    }

    @Transactional
    public MaintenanceRequestResponse convert(Long id, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertAdmin(user);
        MaintenanceRequest row = lockReadable(id, user);
        if (STATUS_CONVERTED.equals(row.getStatus()) || row.getWorkOrder() != null) {
            throw alreadyConverted();
        }
        if (!STATUS_APPROVED.equals(row.getStatus())) {
            throw invalidTransition("Only an approved request can be converted.");
        }
        WorkOrderCreateRequest create = new WorkOrderCreateRequest();
        create.setVehicleId(row.getVehicle().getId());
        create.setSource(WorkOrderService.SOURCE_MANUAL);
        create.setMaintenanceType(WORK_ORDER_TYPE);
        create.setName(row.getName());
        create.setDescription(row.getDescription());
        create.setDiagnosis(row.getDescription());
        create.setPriority(workOrderPriority(row.getPriority()));
        create.setRequestedDate(row.getRequestedAt() == null ? LocalDate.now() : row.getRequestedAt().toLocalDate());
        WorkOrderResponse created = workOrderService.create(create, username);

        WorkOrder workOrder = workOrderRepository.getReferenceById(created.getId());
        row.setWorkOrder(workOrder);
        row.setStatus(STATUS_CONVERTED);
        row.setUpdatedBy(username);
        requestRepository.save(row);
        auditService.log(username, "MAINTENANCE_REQUEST_CONVERTED", "maintenance_requests", row.getId(), null,
                "number=" + row.getRequestNumber() + ", workOrderId=" + created.getId());
        return toResponse(reload(row.getId()));
    }

    private String workOrderPriority(String requestPriority) {
        if (PRIORITY_LOW.equals(requestPriority)) {
            return "LOW";
        }
        if (PRIORITY_HIGH.equals(requestPriority) || PRIORITY_CRITICAL.equals(requestPriority)) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private Vehicle lockVehicle(Long vehicleId, AppUser user) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(this::vehicleRequired);
        entityManager.refresh(vehicle);
        if (Boolean.TRUE.equals(vehicle.getIsDeleted())) {
            throw vehicleRequired();
        }
        try {
            parentAccess.requireVehicle(vehicleId);
        } catch (IllegalArgumentException ex) {
            throw vehicleRequired();
        }
        tenantAccess.assertCompanyAccess(vehicle.getCompanyId());
        assertBranch(vehicle.getBranchId(), user, "Access denied: Vehicle belongs to another branch.");
        return vehicle;
    }

    private MaintenanceRequest requireReadable(Long id) {
        AppUser user = tenantAccess.requireCurrentUser();
        MaintenanceRequest row = requestRepository.findDetailById(id)
                .orElseThrow(this::notFound);
        assertReadable(row, user);
        return row;
    }

    private MaintenanceRequest lockReadable(Long id, AppUser user) {
        MaintenanceRequest row = requestRepository.findByIdForUpdate(id)
                .orElseThrow(this::notFound);
        assertReadable(row, user);
        return row;
    }

    private MaintenanceRequest reload(Long id) {
        return requestRepository.findDetailById(id).orElseThrow(this::notFound);
    }

    private void assertReadable(MaintenanceRequest row, AppUser user) {
        tenantAccess.assertCompanyAccess(row.getCompanyId());
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (isDriverOnly(user)) {
            Driver driver = driverAuthorization.requireLinkedDriver(user);
            driverAuthorization.assertAuthorized(user, driver, row.getVehicle());
            return;
        }
        assertBranch(row.getBranchId(), user, "Access denied: Maintenance request belongs to another branch.");
    }

    private void assertBranch(Long resourceBranchId, AppUser user, String message) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getBranchId() != null && resourceBranchId != null
                && !user.getBranchId().equals(resourceBranchId)) {
            throw new AccessDeniedException(message);
        }
    }

    private void assertAdmin(AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getRoles() == null || user.getRoles().stream().map(AppRole::getCode).noneMatch(ADMIN_ROLES::contains)) {
            throw new AccessDeniedException("Access denied: maintenance request management requires COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    private boolean isDriverOnly(AppUser user) {
        return driverAuthorization.isDriverRole(user) && !tenantAccess.isSuperAdmin(user)
                && (user.getRoles() == null
                || user.getRoles().stream().map(AppRole::getCode).noneMatch(ADMIN_ROLES::contains));
    }

    private String requireTitle(String raw) {
        String title = raw == null ? "" : raw.trim();
        if (title.isEmpty() || title.length() > 150) {
            throw failure(
                    "Title Required",
                    "MAINTENANCE_REQUEST_TITLE_REQUIRED",
                    "MAINTENANCE_REQUEST_TITLE_REQUIRED: A title is required and must be 150 characters or fewer.",
                    "Enter a short summary of the problem.");
        }
        return title;
    }

    private String requireDescription(String raw) {
        String description = raw == null ? "" : raw.trim();
        if (description.isEmpty()) {
            throw failure(
                    "Description Required",
                    "MAINTENANCE_REQUEST_DESCRIPTION_REQUIRED",
                    "MAINTENANCE_REQUEST_DESCRIPTION_REQUIRED: A description is required.",
                    "Describe the issue that was reported.");
        }
        return description;
    }

    private String requirePriority(String raw) {
        String priority = raw == null || raw.isBlank() ? PRIORITY_MEDIUM : raw.trim().toUpperCase(Locale.ROOT);
        if (!PRIORITIES.contains(priority)) {
            throw failure(
                    "Invalid Priority",
                    "MAINTENANCE_REQUEST_PRIORITY_INVALID",
                    "MAINTENANCE_REQUEST_PRIORITY_INVALID: Priority must be LOW, MEDIUM, HIGH, or CRITICAL.",
                    "Select a valid priority.");
        }
        return priority;
    }

    private BigDecimal scaleOdometer(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MaintenanceRequestResponse toResponse(MaintenanceRequest row) {
        MaintenanceRequestResponse dto = new MaintenanceRequestResponse();
        dto.setId(row.getId());
        dto.setRequestNumber(row.getRequestNumber());
        dto.setCompanyId(row.getCompanyId());
        dto.setBranchId(row.getBranchId());
        dto.setStatus(row.getStatus());
        dto.setPriority(row.getPriority());
        dto.setTitle(row.getName());
        dto.setDescription(row.getDescription());
        dto.setRequestedAt(row.getRequestedAt());
        dto.setReportedOdometerKm(row.getReportedOdometerKm());
        dto.setReviewedBy(row.getReviewedBy());
        dto.setReviewedAt(row.getReviewedAt());
        dto.setReviewRemarks(row.getReviewRemarks());
        dto.setApprovedBy(row.getApprovedBy());
        dto.setApprovedAt(row.getApprovedAt());
        dto.setCancelledBy(row.getCancelledBy());
        dto.setCancelledAt(row.getCancelledAt());
        dto.setCancellationReason(row.getCancellationReason());
        dto.setCreatedBy(row.getCreatedBy());
        dto.setCreatedDate(row.getCreatedDate());
        dto.setUpdatedBy(row.getUpdatedBy());
        dto.setUpdatedDate(row.getUpdatedDate());
        dto.setVersion(row.getVersion());
        if (row.getVehicle() != null) {
            dto.setVehicleId(row.getVehicle().getId());
            dto.setVehicleCode(row.getVehicle().getCode());
            dto.setVehicleName(row.getVehicle().getName());
            dto.setVehicleRegistrationNumber(row.getVehicle().getCode());
        }
        if (row.getRequestedBy() != null) {
            dto.setRequestedByUserId(row.getRequestedBy().getId());
            dto.setRequestedByUsername(row.getRequestedBy().getUsername());
        }
        if (row.getDriver() != null) {
            dto.setDriverId(row.getDriver().getId());
            dto.setDriverName(row.getDriver().getName());
        }
        if (row.getWorkOrder() != null) {
            dto.setWorkOrderId(row.getWorkOrder().getId());
            dto.setWorkOrderNumber(row.getWorkOrder().getWorkOrderNumber());
        }
        return dto;
    }

    private BusinessValidationException notFound() {
        return failure(
                "Maintenance Request Not Found",
                "MAINTENANCE_REQUEST_NOT_FOUND",
                "MAINTENANCE_REQUEST_NOT_FOUND: The maintenance request was not found.",
                "Refresh the list and select a request you can access.");
    }

    private BusinessValidationException notEditable() {
        return failure(
                "Maintenance Request Not Editable",
                "MAINTENANCE_REQUEST_NOT_EDITABLE",
                "MAINTENANCE_REQUEST_NOT_EDITABLE: This request cannot be edited in its current state.",
                "Only an open request can be edited.");
    }

    private BusinessValidationException invalidTransition(String message) {
        return failure(
                "Invalid Maintenance Request Transition",
                "MAINTENANCE_REQUEST_INVALID_TRANSITION",
                "MAINTENANCE_REQUEST_INVALID_TRANSITION: " + message,
                "Refresh the request and use the action allowed for its status.");
    }

    private BusinessValidationException alreadyConverted() {
        return failure(
                "Maintenance Request Already Converted",
                "MAINTENANCE_REQUEST_ALREADY_CONVERTED",
                "MAINTENANCE_REQUEST_ALREADY_CONVERTED: This request is already linked to a work order.",
                "Open the existing work order. Do not convert the request again.");
    }

    private BusinessValidationException vehicleRequired() {
        return failure(
                "Vehicle Required",
                "MAINTENANCE_REQUEST_VEHICLE_REQUIRED",
                "MAINTENANCE_REQUEST_VEHICLE_REQUIRED: The vehicle is missing or is not available.",
                "Select an active vehicle.");
    }

    private BusinessValidationException driverAuthorizationFailure() {
        return new BusinessValidationException(
                "Vehicle Not Authorized",
                "DRIVER_VEHICLE_NOT_AUTHORIZED",
                "DRIVER_VEHICLE_NOT_AUTHORIZED: This driver is not assigned to the selected vehicle.",
                "Select a vehicle on your active assignment.");
    }

    private BusinessValidationException failure(String title, String code, String message, String action) {
        return new BusinessValidationException(title, code, message, action);
    }
}
