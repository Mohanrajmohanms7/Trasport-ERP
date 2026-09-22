package com.transport.erp.service;

import com.transport.erp.dto.WorkOrderLabourRequest;
import com.transport.erp.dto.WorkOrderPartRequest;
import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.model.WorkOrderLabour;
import com.transport.erp.model.WorkOrderPart;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.SparePartRepository;
import com.transport.erp.repository.WorkOrderLabourRepository;
import com.transport.erp.repository.WorkOrderPartRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkOrderLineService {

    private static final Set<String> WRITE_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");
    private static final Set<String> EDITABLE = Set.of("OPEN", "IN_PROGRESS");

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderPartRepository partRepository;

    @Autowired
    private WorkOrderLabourRepository labourRepository;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    @Transactional
    public WorkOrderResponse addPart(Long workOrderId, WorkOrderPartRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockEditable(workOrderId, user);
        WorkOrderPart line = new WorkOrderPart();
        line.setWorkOrder(order);
        applyPart(line, order, request, true);
        line.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 20));
        line.setCreatedBy(username);
        line.setUpdatedBy(username);
        line.setIsDeleted(false);
        line.setStatus("ACTIVE");
        line = partRepository.saveAndFlush(line);
        line.setCode("WOP-" + String.format("%06d", line.getId()));
        partRepository.save(line);
        auditService.log(username, "WORK_ORDER_PART_ADDED", "work_order_parts", line.getId(), null,
                order.getWorkOrderNumber());
        return workOrderService.get(workOrderId);
    }

    @Transactional
    public WorkOrderResponse updatePart(Long workOrderId, Long lineId, WorkOrderPartRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockEditable(workOrderId, user);
        WorkOrderPart line = partRepository.findActiveLine(lineId, workOrderId)
                .orElseThrow(this::lineNotFound);
        applyPart(line, order, request, false);
        line.setUpdatedBy(username);
        partRepository.save(line);
        auditService.log(username, "WORK_ORDER_PART_UPDATED", "work_order_parts", line.getId(), null,
                order.getWorkOrderNumber());
        return workOrderService.get(workOrderId);
    }

    @Transactional
    public WorkOrderResponse removePart(Long workOrderId, Long lineId, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockEditable(workOrderId, user);
        WorkOrderPart line = partRepository.findActiveLine(lineId, workOrderId)
                .orElseThrow(this::lineNotFound);
        line.setIsDeleted(true);
        line.setUpdatedBy(username);
        partRepository.save(line);
        auditService.log(username, "WORK_ORDER_PART_REMOVED", "work_order_parts", line.getId(), null,
                order.getWorkOrderNumber());
        return workOrderService.get(workOrderId);
    }

    @Transactional
    public WorkOrderResponse addLabour(Long workOrderId, WorkOrderLabourRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockEditable(workOrderId, user);
        WorkOrderLabour line = new WorkOrderLabour();
        line.setWorkOrder(order);
        applyLabour(line, order, request);
        line.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 20));
        line.setCreatedBy(username);
        line.setUpdatedBy(username);
        line.setIsDeleted(false);
        line.setStatus("ACTIVE");
        line = labourRepository.saveAndFlush(line);
        line.setCode("WOL-" + String.format("%06d", line.getId()));
        labourRepository.save(line);
        auditService.log(username, "WORK_ORDER_LABOUR_ADDED", "work_order_labour", line.getId(), null,
                order.getWorkOrderNumber());
        return workOrderService.get(workOrderId);
    }

    @Transactional
    public WorkOrderResponse updateLabour(Long workOrderId, Long lineId, WorkOrderLabourRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockEditable(workOrderId, user);
        WorkOrderLabour line = labourRepository.findActiveLine(lineId, workOrderId)
                .orElseThrow(this::lineNotFound);
        applyLabour(line, order, request);
        line.setUpdatedBy(username);
        labourRepository.save(line);
        auditService.log(username, "WORK_ORDER_LABOUR_UPDATED", "work_order_labour", line.getId(), null,
                order.getWorkOrderNumber());
        return workOrderService.get(workOrderId);
    }

    @Transactional
    public WorkOrderResponse removeLabour(Long workOrderId, Long lineId, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        WorkOrder order = lockEditable(workOrderId, user);
        WorkOrderLabour line = labourRepository.findActiveLine(lineId, workOrderId)
                .orElseThrow(this::lineNotFound);
        line.setIsDeleted(true);
        line.setUpdatedBy(username);
        labourRepository.save(line);
        auditService.log(username, "WORK_ORDER_LABOUR_REMOVED", "work_order_labour", line.getId(), null,
                order.getWorkOrderNumber());
        return workOrderService.get(workOrderId);
    }

    private void applyPart(WorkOrderPart line, WorkOrder order, WorkOrderPartRequest request, boolean creating) {
        if (request == null || request.getSparePartId() == null) {
            throw partInvalid();
        }
        SparePart part = sparePartRepository.findById(request.getSparePartId())
                .orElseThrow(this::partInvalid);
        if (Boolean.TRUE.equals(part.getIsDeleted())
                || part.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(part.getStatus())
                || part.getDefaultUom() == null
                || !order.getCompanyId().equals(part.getCompanyId())) {
            throw partInvalid();
        }
        BigDecimal quantity = requirePositive(request.getQuantity(), 3, this::quantityInvalid);
        BigDecimal rate = request.getUnitRate();
        if (rate == null) {
            if (creating || line.getUnitRate() == null) {
                rate = part.getDefaultRate() == null ? BigDecimal.ZERO : part.getDefaultRate();
            } else {
                rate = line.getUnitRate();
            }
        }
        rate = requireNonNegative(rate, 2, this::rateInvalid);
        boolean partChanged = line.getSparePart() == null || !part.getId().equals(line.getSparePart().getId());
        line.setSparePart(part);
        if (creating || partChanged) {
            line.setUom(part.getDefaultUom());
        }
        line.setQuantity(quantity);
        line.setUnitRate(rate);
        line.setLineTotal(lineTotal(quantity, rate));
        line.setNotes(trimToNull(request.getNotes()));
        line.setName(truncate(part.getName(), 150));
        line.setDescription(part.getDescription());
    }

    private void applyLabour(WorkOrderLabour line, WorkOrder order, WorkOrderLabourRequest request) {
        if (request == null) {
            throw descriptionRequired();
        }
        String description = request.getDescription() != null ? request.getDescription().trim() : "";
        if (description.isEmpty()) {
            throw descriptionRequired();
        }
        BigDecimal hours = requirePositive(request.getHours(), 2, this::hoursInvalid);
        BigDecimal rate = requireNonNegative(request.getRate(), 2, this::labourRateInvalid);
        AppUser worker = null;
        if (request.getAppUserId() != null) {
            worker = userRepository.findById(request.getAppUserId())
                    .filter(row -> !Boolean.TRUE.equals(row.getIsDeleted()))
                    .filter(row -> order.getCompanyId().equals(row.getCompanyId()))
                    .orElseThrow(this::labourUserInvalid);
        }
        line.setAppUser(worker);
        line.setDescription(description);
        line.setName(truncate(description, 150));
        line.setHours(hours);
        line.setRate(rate);
        line.setLineTotal(lineTotal(hours, rate));
        line.setWorkDate(request.getWorkDate());
        line.setNotes(trimToNull(request.getNotes()));
    }

    private WorkOrder lockEditable(Long workOrderId, AppUser user) {
        WorkOrder order = workOrderRepository.findByIdForUpdate(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));
        tenantAccess.assertCompanyAccess(order.getCompanyId());
        if (!tenantAccess.isSuperAdmin(user)
                && user.getBranchId() != null
                && order.getBranchId() != null
                && !user.getBranchId().equals(order.getBranchId())) {
            throw new AccessDeniedException("Access denied: Work order belongs to another branch.");
        }
        if (!EDITABLE.contains(order.getStatus())) {
            throw failure(
                    "Work Order Not Editable",
                    "WORK_ORDER_NOT_EDITABLE",
                    "Parts and labour can be changed only while the work order is OPEN or IN_PROGRESS.",
                    "Completed and cancelled work orders are read-only.");
        }
        return order;
    }

    private BigDecimal requirePositive(BigDecimal value, int scale, java.util.function.Supplier<BusinessValidationException> error) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw error.get();
        }
        try {
            return value.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw error.get();
        }
    }

    private BigDecimal requireNonNegative(BigDecimal value, int scale, java.util.function.Supplier<BusinessValidationException> error) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw error.get();
        }
        try {
            return value.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            return value.setScale(scale, RoundingMode.HALF_UP);
        }
    }

    static BigDecimal lineTotal(BigDecimal quantity, BigDecimal rate) {
        return quantity.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private void assertWriteAccess(AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getRoles() == null || user.getRoles().stream().map(AppRole::getCode).noneMatch(WRITE_ROLES::contains)) {
            throw new AccessDeniedException("Access denied: work order updates require COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "Line";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "Line";
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BusinessValidationException partInvalid() {
        return failure(
                "Invalid Spare Part",
                "SPARE_PART_INVALID",
                "The spare part must be an active part in the work order company.",
                "Select an active spare part from this company.");
    }

    private BusinessValidationException quantityInvalid() {
        return failure(
                "Invalid Part Quantity",
                "WORK_ORDER_PART_QUANTITY_INVALID",
                "Part quantity must be greater than zero.",
                "Enter a quantity greater than zero.");
    }

    private BusinessValidationException rateInvalid() {
        return failure(
                "Invalid Part Rate",
                "WORK_ORDER_PART_RATE_INVALID",
                "Part unit rate cannot be negative.",
                "Enter zero or a positive unit rate.");
    }

    private BusinessValidationException descriptionRequired() {
        return failure(
                "Labour Description Required",
                "WORK_ORDER_LABOUR_DESCRIPTION_REQUIRED",
                "Labour description is required.",
                "Describe the labour.");
    }

    private BusinessValidationException labourUserInvalid() {
        return failure(
                "Invalid Labour User",
                "WORK_ORDER_LABOUR_USER_INVALID",
                "The labour user must belong to the work order company.",
                "Select a user from this company, or leave the person blank.");
    }

    private BusinessValidationException hoursInvalid() {
        return failure(
                "Invalid Labour Hours",
                "WORK_ORDER_LABOUR_HOURS_INVALID",
                "Labour hours must be greater than zero.",
                "Enter hours greater than zero.");
    }

    private BusinessValidationException labourRateInvalid() {
        return failure(
                "Invalid Labour Rate",
                "WORK_ORDER_LABOUR_RATE_INVALID",
                "Labour rate cannot be negative.",
                "Enter zero or a positive rate.");
    }

    private BusinessValidationException lineNotFound() {
        return failure(
                "Work Order Line Not Found",
                "WORK_ORDER_LINE_NOT_FOUND",
                "The work order line was not found.",
                "Refresh the work order and choose an existing line.");
    }

    private BusinessValidationException failure(String title, String code, String message, String action) {
        return new BusinessValidationException(title, code, code + ": " + message, action);
    }
}
