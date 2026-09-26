package com.transport.erp.service;

import com.transport.erp.dto.StockReceiptRequest;
import com.transport.erp.dto.StockReceiptResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.InventoryTransaction;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.Supplier;
import com.transport.erp.model.Warehouse;
import com.transport.erp.model.WarehouseStock;
import com.transport.erp.repository.InventoryTransactionRepository;
import com.transport.erp.repository.SparePartRepository;
import com.transport.erp.repository.SupplierRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class InventoryReceiptService {

    @Autowired
    private InventoryValuationService valuationService;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    @Transactional
    public StockReceiptResponse receive(StockReceiptRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        warehouseService.assertWriteAccess(user);
        if (request == null) {
            throw invalid("INVENTORY_RECEIPT_WAREHOUSE_REQUIRED", "Warehouse, spare part, and quantity are required.");
        }
        if (request.getWarehouseId() == null) {
            throw invalid("INVENTORY_RECEIPT_WAREHOUSE_REQUIRED", "A warehouse is required.");
        }
        if (request.getSparePartId() == null) {
            throw invalid("INVENTORY_RECEIPT_SPARE_PART_REQUIRED", "A spare part is required.");
        }
        BigDecimal quantity = requireQuantity(request.getQuantity());
        BigDecimal unitRate = optionalUnitRate(request.getUnitRate());
        String externalReference = trimToNull(request.getReferenceNumber());

        Warehouse warehouse = warehouseService.requireActive(request.getWarehouseId(), user);
        SparePart part = requireUsablePart(request.getSparePartId(), warehouse.getCompanyId());
        Supplier supplier = optionalSupplier(request.getSupplierId(), warehouse.getCompanyId());
        if (externalReference != null
                && transactionRepository.countReceiptExternalReference(warehouse.getCompanyId(), externalReference) > 0) {
            throw invalid("INVENTORY_RECEIPT_REFERENCE_DUPLICATE", "This receipt reference was already used.");
        }

        WarehouseStock stock = inventoryService.increaseAvailable(warehouse, part, quantity, username);
        valuationService.blendCost(stock, quantity, unitRate);
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setCompanyId(warehouse.getCompanyId());
        transaction.setBranchId(warehouse.getBranchId());
        transaction.setWarehouse(warehouse);
        transaction.setSparePart(part);
        transaction.setSupplier(supplier);
        transaction.setTransactionType(InventoryTransaction.TYPE_RECEIPT);
        transaction.setQuantity(quantity);
        transaction.setUnitRate(unitRate);
        transaction.setExternalReference(externalReference);
        transaction.setReferenceType("WAREHOUSE_STOCK");
        transaction.setReferenceId(stock.getId());
        transaction.setDescription(trimToNull(request.getDescription()));
        transaction.setCode("TMP");
        transaction.setName("Receipt");
        transaction.setStatus("ACTIVE");
        transaction.setCreatedBy(username);
        transaction.setUpdatedBy(username);
        transaction.setIsDeleted(false);
        try {
            transaction = transactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateExternalReference(ex)) {
                throw invalid("INVENTORY_RECEIPT_REFERENCE_DUPLICATE", "This receipt reference was already used.");
            }
            throw ex;
        }
        transaction.setCode("RC-" + String.format("%06d", transaction.getId()));
        transaction = transactionRepository.save(transaction);
        valuationService.postReceipt(transaction, username);

        auditService.log(username, "INVENTORY_RECEIPT_CREATED", "inventory_transactions", transaction.getId(), null,
                "warehouseId=" + warehouse.getId() + ", sparePartId=" + part.getId() + ", quantity=" + quantity);
        return toResponse(transaction, warehouse, part, supplier, stock.getAvailableQuantity());
    }

    private SparePart requireUsablePart(Long sparePartId, Long companyId) {
        SparePart part = sparePartRepository.findDetailById(sparePartId)
                .orElseThrow(() -> invalid("INVENTORY_RECEIPT_SPARE_PART_INVALID", "Spare part was not found."));
        if (Boolean.TRUE.equals(part.getIsDeleted())) {
            throw invalid("INVENTORY_RECEIPT_SPARE_PART_INVALID", "A deleted spare part cannot receive stock.");
        }
        if (part.getCompanyId() == null || !part.getCompanyId().equals(companyId)) {
            throw invalid("INVENTORY_RECEIPT_SPARE_PART_INVALID", "The spare part does not belong to this company.");
        }
        return part;
    }

    private Supplier optionalSupplier(Long supplierId, Long companyId) {
        if (supplierId == null) {
            return null;
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(row -> !Boolean.TRUE.equals(row.getIsDeleted()))
                .orElseThrow(() -> invalid("INVENTORY_RECEIPT_SUPPLIER_INVALID", "Supplier was not found."));
        if (supplier.getCompanyId() == null || !supplier.getCompanyId().equals(companyId)) {
            throw invalid("INVENTORY_RECEIPT_SUPPLIER_INVALID", "The supplier does not belong to this company.");
        }
        return supplier;
    }

    private BigDecimal requireQuantity(BigDecimal raw) {
        if (raw == null) {
            throw invalid("INVENTORY_RECEIPT_QUANTITY_INVALID", "Receipt quantity must be greater than zero.");
        }
        if (raw.compareTo(BigDecimal.ZERO) == 0) {
            throw invalid("INVENTORY_RECEIPT_QUANTITY_INVALID", "Receipt quantity must be greater than zero.");
        }
        if (raw.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("INVENTORY_RECEIPT_QUANTITY_INVALID", "Receipt quantity cannot be negative.");
        }
        try {
            return raw.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw invalid("INVENTORY_RECEIPT_QUANTITY_INVALID", "Receipt quantity must be greater than zero.");
        }
    }

    private BigDecimal optionalUnitRate(BigDecimal raw) {
        if (raw == null) {
            return null;
        }
        if (raw.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("INVENTORY_RECEIPT_UNIT_RATE_INVALID", "Unit rate cannot be negative.");
        }
        try {
            return raw.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw invalid("INVENTORY_RECEIPT_UNIT_RATE_INVALID", "Unit rate cannot be negative.");
        }
    }

    private StockReceiptResponse toResponse(
            InventoryTransaction transaction,
            Warehouse warehouse,
            SparePart part,
            Supplier supplier,
            BigDecimal availableQuantity) {
        StockReceiptResponse dto = new StockReceiptResponse();
        dto.setTransactionId(transaction.getId());
        dto.setTransactionCode(transaction.getCode());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setWarehouseId(warehouse.getId());
        dto.setWarehouseCode(warehouse.getCode());
        dto.setWarehouseName(warehouse.getName());
        dto.setSparePartId(part.getId());
        dto.setSparePartCode(part.getCode());
        dto.setSparePartName(part.getName());
        dto.setQuantity(transaction.getQuantity());
        dto.setUnitRate(transaction.getUnitRate());
        dto.setExternalReference(transaction.getExternalReference());
        dto.setAvailableQuantity(availableQuantity);
        dto.setCreatedDate(transaction.getCreatedDate());
        dto.setCreatedBy(transaction.getCreatedBy());
        if (supplier != null) {
            dto.setSupplierId(supplier.getId());
            dto.setSupplierCode(supplier.getCode());
            dto.setSupplierName(supplier.getName());
        }
        return dto;
    }

    private boolean isDuplicateExternalReference(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("uk_inventory_tx_company_receipt_ext_ref");
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BusinessValidationException invalid(String code, String message) {
        return new BusinessValidationException(
                "Invalid Inventory",
                code,
                code + ": " + message,
                "Check the warehouse, spare part, and quantity.");
    }
}
