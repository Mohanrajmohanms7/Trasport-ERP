package com.transport.erp.service;

import com.transport.erp.dto.StockIssueRequest;
import com.transport.erp.dto.StockMovementResponse;
import com.transport.erp.dto.StockReturnRequest;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.InventoryTransaction;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.Warehouse;
import com.transport.erp.model.WarehouseStock;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.model.WorkOrderPart;
import com.transport.erp.repository.InventoryTransactionRepository;
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

@Service
public class InventoryIssueService {

    private static final Set<String> MOVABLE = Set.of("OPEN", "IN_PROGRESS");

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderPartRepository workOrderPartRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    @Transactional
    public StockMovementResponse issue(StockIssueRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        warehouseService.assertWriteAccess(user);
        Context context = lockContext(request == null ? null : request.getWorkOrderId(),
                request == null ? null : request.getWorkOrderPartId(),
                request == null ? null : request.getWarehouseId(),
                user);
        BigDecimal quantity = requireQuantity(request == null ? null : request.getQuantity(), false);
        BigDecimal issued = zero(context.line.getIssuedQuantity());
        BigDecimal requested = context.line.getQuantity();
        if (issued.add(quantity).compareTo(requested) > 0) {
            throw invalid("INVENTORY_QUANTITY_INVALID",
                    "Issue quantity exceeds the remaining work order quantity.");
        }

        WarehouseStock stock = inventoryService.decreaseAvailable(
                context.warehouse, context.part, quantity, username);
        context.line.setIssuedQuantity(issued.add(quantity));
        context.line.setUpdatedBy(username);
        workOrderPartRepository.saveAndFlush(context.line);

        InventoryTransaction transaction = saveMovement(
                context, InventoryTransaction.TYPE_ISSUE, "IS-", "Issue", quantity, username);
        auditService.log(username, "INVENTORY_ISSUED", "inventory_transactions", transaction.getId(), null,
                context.order.getWorkOrderNumber()
                        + ", warehouseId=" + context.warehouse.getId()
                        + ", quantity=" + quantity);
        return toResponse(transaction, context, stock.getAvailableQuantity());
    }

    @Transactional
    public StockMovementResponse returnStock(StockReturnRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        warehouseService.assertWriteAccess(user);
        Context context = lockContext(request == null ? null : request.getWorkOrderId(),
                request == null ? null : request.getWorkOrderPartId(),
                request == null ? null : request.getWarehouseId(),
                user);
        BigDecimal quantity = requireQuantity(request == null ? null : request.getQuantity(), true);
        BigDecimal issuedAtWarehouse = zero(transactionRepository.sumQuantity(
                context.warehouse.getId(), context.line.getId(), InventoryTransaction.TYPE_ISSUE));
        BigDecimal returnedAtWarehouse = zero(transactionRepository.sumQuantity(
                context.warehouse.getId(), context.line.getId(), InventoryTransaction.TYPE_RETURN));
        BigDecimal returnableAtWarehouse = issuedAtWarehouse.subtract(returnedAtWarehouse);
        if (quantity.compareTo(returnableAtWarehouse) > 0) {
            throw invalid("INVENTORY_RETURN_EXCEEDS_ISSUED",
                    "Return quantity exceeds the remaining issued quantity.");
        }
        BigDecimal issued = zero(context.line.getIssuedQuantity());
        BigDecimal returned = zero(context.line.getReturnedQuantity());
        if (returned.add(quantity).compareTo(issued) > 0) {
            throw invalid("INVENTORY_RETURN_EXCEEDS_ISSUED",
                    "Return quantity exceeds the remaining issued quantity.");
        }

        WarehouseStock stock = inventoryService.increaseAvailable(
                context.warehouse, context.part, quantity, username);
        context.line.setReturnedQuantity(returned.add(quantity));
        context.line.setUpdatedBy(username);
        workOrderPartRepository.saveAndFlush(context.line);

        InventoryTransaction transaction = saveMovement(
                context, InventoryTransaction.TYPE_RETURN, "RT-", "Return", quantity, username);
        auditService.log(username, "INVENTORY_RETURNED", "inventory_transactions", transaction.getId(), null,
                context.order.getWorkOrderNumber()
                        + ", warehouseId=" + context.warehouse.getId()
                        + ", quantity=" + quantity);
        return toResponse(transaction, context, stock.getAvailableQuantity());
    }

    private Context lockContext(Long workOrderId, Long workOrderPartId, Long warehouseId, AppUser user) {
        if (workOrderId == null) {
            throw invalid("INVENTORY_WORK_ORDER_REQUIRED", "A work order is required.");
        }
        if (workOrderPartId == null) {
            throw invalid("INVENTORY_WORK_ORDER_PART_REQUIRED", "A work order part is required.");
        }
        if (warehouseId == null) {
            throw invalid("INVENTORY_WAREHOUSE_REQUIRED", "A warehouse is required.");
        }
        WorkOrder order = workOrderRepository.findByIdForUpdate(workOrderId)
                .orElseThrow(() -> invalid("INVENTORY_WORK_ORDER_REQUIRED", "Work order was not found."));
        tenantAccess.assertCompanyAccess(order.getCompanyId());
        assertBranch(order.getBranchId(), user, "Access denied: Work order belongs to another branch.");
        if (!MOVABLE.contains(order.getStatus())) {
            throw invalid("INVENTORY_WORK_ORDER_INVALID_STATE",
                    "Stock can be issued or returned only while the work order is OPEN or IN_PROGRESS.");
        }
        WorkOrderPart line = workOrderPartRepository.findActiveLineForUpdate(workOrderPartId, workOrderId)
                .orElseThrow(() -> invalid("INVENTORY_WORK_ORDER_PART_REQUIRED",
                        "The work order part was not found on this work order."));
        SparePart part = line.getSparePart();
        if (part == null || Boolean.TRUE.equals(part.getIsDeleted())) {
            throw invalid("INVENTORY_SPARE_PART_REQUIRED", "The work order part spare part is not available.");
        }
        Warehouse warehouse = warehouseService.requireUsable(warehouseId, user);
        if (warehouse.getCompanyId() == null || !warehouse.getCompanyId().equals(order.getCompanyId())) {
            throw new AccessDeniedException("Access denied to another company's data");
        }
        if (order.getBranchId() != null && warehouse.getBranchId() != null
                && !order.getBranchId().equals(warehouse.getBranchId())) {
            throw invalid("WAREHOUSE_BRANCH_INVALID",
                    "The warehouse must belong to the work order branch.");
        }
        if (part.getCompanyId() == null || !part.getCompanyId().equals(order.getCompanyId())) {
            throw new AccessDeniedException("Access denied to another company's data");
        }
        return new Context(order, line, part, warehouse);
    }

    private InventoryTransaction saveMovement(
            Context context,
            String type,
            String prefix,
            String name,
            BigDecimal quantity,
            String username) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setCompanyId(context.warehouse.getCompanyId());
        transaction.setBranchId(context.warehouse.getBranchId());
        transaction.setWarehouse(context.warehouse);
        transaction.setSparePart(context.part);
        transaction.setWorkOrder(context.order);
        transaction.setWorkOrderPart(context.line);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setReferenceType("WORK_ORDER_PART");
        transaction.setReferenceId(context.line.getId());
        transaction.setCode("TMP");
        transaction.setName(name);
        transaction.setStatus("ACTIVE");
        transaction.setCreatedBy(username);
        transaction.setUpdatedBy(username);
        transaction.setIsDeleted(false);
        transaction = transactionRepository.saveAndFlush(transaction);
        transaction.setCode(prefix + String.format("%06d", transaction.getId()));
        return transactionRepository.save(transaction);
    }

    private StockMovementResponse toResponse(
            InventoryTransaction transaction, Context context, BigDecimal availableQuantity) {
        BigDecimal issued = zero(context.line.getIssuedQuantity());
        BigDecimal returned = zero(context.line.getReturnedQuantity());
        BigDecimal requested = context.line.getQuantity();
        StockMovementResponse dto = new StockMovementResponse();
        dto.setTransactionId(transaction.getId());
        dto.setTransactionCode(transaction.getCode());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setWorkOrderId(context.order.getId());
        dto.setWorkOrderNumber(context.order.getWorkOrderNumber());
        dto.setWorkOrderPartId(context.line.getId());
        dto.setSparePartId(context.part.getId());
        dto.setSparePartCode(context.part.getCode());
        dto.setSparePartName(context.part.getName());
        dto.setWarehouseId(context.warehouse.getId());
        dto.setWarehouseCode(context.warehouse.getCode());
        dto.setWarehouseName(context.warehouse.getName());
        dto.setQuantity(transaction.getQuantity());
        dto.setIssuedQuantity(issued);
        dto.setReturnedQuantity(returned);
        dto.setNetIssuedQuantity(issued.subtract(returned));
        dto.setRemainingToIssue(requested.subtract(issued));
        dto.setReturnableQuantity(issued.subtract(returned));
        dto.setAvailableQuantity(availableQuantity);
        return dto;
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

    private BigDecimal requireQuantity(BigDecimal raw, boolean returning) {
        String invalidCode = returning ? "INVENTORY_RETURN_QUANTITY_INVALID" : "INVENTORY_QUANTITY_INVALID";
        String zeroMessage = returning
                ? "Return quantity must be greater than zero."
                : "Issue quantity must be greater than zero.";
        String negativeMessage = returning
                ? "Return quantity cannot be negative."
                : "Issue quantity cannot be negative.";
        if (raw == null) {
            throw invalid(invalidCode, zeroMessage);
        }
        if (raw.compareTo(BigDecimal.ZERO) == 0) {
            throw invalid(invalidCode, zeroMessage);
        }
        if (raw.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid(invalidCode, negativeMessage);
        }
        try {
            return raw.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw invalid(invalidCode, zeroMessage);
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BusinessValidationException invalid(String code, String message) {
        return new BusinessValidationException(
                "Invalid Inventory",
                code,
                code + ": " + message,
                "Check the warehouse, work order part, and quantity.");
    }

    private static final class Context {
        private final WorkOrder order;
        private final WorkOrderPart line;
        private final SparePart part;
        private final Warehouse warehouse;

        private Context(WorkOrder order, WorkOrderPart line, SparePart part, Warehouse warehouse) {
            this.order = order;
            this.line = line;
            this.part = part;
            this.warehouse = warehouse;
        }
    }
}
