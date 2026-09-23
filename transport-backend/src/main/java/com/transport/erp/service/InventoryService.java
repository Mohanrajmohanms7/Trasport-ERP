package com.transport.erp.service;

import com.transport.erp.dto.InventoryTransactionResponse;
import com.transport.erp.dto.OpeningBalanceRequest;
import com.transport.erp.dto.WarehouseStockResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.InventoryTransaction;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.Warehouse;
import com.transport.erp.model.WarehouseStock;
import com.transport.erp.repository.InventoryTransactionRepository;
import com.transport.erp.repository.SparePartRepository;
import com.transport.erp.repository.WarehouseStockRepository;
import com.transport.erp.security.TenantAccessService;
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
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    static final String UNIQUE_STOCK = "uk_warehouse_stock_warehouse_part";

    @Autowired
    private WarehouseStockRepository stockRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    @Transactional(readOnly = true)
    public Page<WarehouseStockResponse> listStock(
            Long requestedCompanyId,
            Long requestedBranchId,
            Long warehouseId,
            Long sparePartId,
            String code,
            Pageable pageable) {
        AppUser user = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return Page.empty(pageable);
        }
        Long branchFilter = listBranchFilter(user, requestedBranchId);
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "id"));
        Page<Long> ids = stockRepository.searchIds(
                companyId, warehouseId, sparePartId, branchFilter, blankToEmpty(code), sorted);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), sorted, ids.getTotalElements());
        }
        Map<Long, WarehouseStock> byId = stockRepository.findDetailsByIds(ids.getContent()).stream()
                .collect(Collectors.toMap(WarehouseStock::getId, Function.identity(), (a, b) -> a));
        List<WarehouseStockResponse> content = ids.getContent().stream()
                .map(byId::get)
                .filter(row -> row != null)
                .map(this::toStockResponse)
                .toList();
        return new PageImpl<>(content, sorted, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public WarehouseStockResponse getStock(Long id) {
        AppUser user = tenantAccess.requireCurrentUser();
        WarehouseStock stock = stockRepository.findDetailById(id)
                .orElseThrow(() -> invalid("STOCK_NOT_FOUND", "Stock balance was not found."));
        assertReadable(stock.getCompanyId(), stock.getBranchId(), user);
        return toStockResponse(stock);
    }

    @Transactional
    public WarehouseStockResponse createOpeningBalance(OpeningBalanceRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        warehouseService.assertWriteAccess(user);
        if (request == null) {
            throw invalid("OPENING_BALANCE_WAREHOUSE_REQUIRED", "Warehouse, spare part, and quantity are required.");
        }
        if (request.getWarehouseId() == null) {
            throw invalid("OPENING_BALANCE_WAREHOUSE_REQUIRED", "A warehouse is required.");
        }
        if (request.getSparePartId() == null) {
            throw invalid("OPENING_BALANCE_SPARE_PART_REQUIRED", "A spare part is required.");
        }
        BigDecimal quantity = requireOpeningQuantity(request.getQuantity());

        Warehouse warehouse = warehouseService.requireUsable(request.getWarehouseId(), user);
        SparePart part = requireUsablePart(request.getSparePartId(), warehouse.getCompanyId());

        WarehouseStock stock = addQuantity(warehouse, part, quantity, username);
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setCompanyId(warehouse.getCompanyId());
        transaction.setBranchId(warehouse.getBranchId());
        transaction.setWarehouse(warehouse);
        transaction.setSparePart(part);
        transaction.setTransactionType(InventoryTransaction.TYPE_OPENING_BALANCE);
        transaction.setQuantity(quantity);
        transaction.setReferenceType("WAREHOUSE_STOCK");
        transaction.setReferenceId(stock.getId());
        transaction.setDescription(trimToNull(request.getDescription()));
        transaction.setCode("TMP");
        transaction.setName("Opening Balance");
        transaction.setStatus("ACTIVE");
        transaction.setCreatedBy(username);
        transaction.setUpdatedBy(username);
        transaction.setIsDeleted(false);
        transaction = transactionRepository.saveAndFlush(transaction);
        transaction.setCode("OB-" + String.format("%06d", transaction.getId()));
        transactionRepository.save(transaction);

        auditService.log(username, "OPENING_BALANCE_CREATED", "inventory_transactions", transaction.getId(), null,
                "warehouseId=" + warehouse.getId() + ", sparePartId=" + part.getId() + ", quantity=" + quantity);
        return toStockResponse(stockRepository.findDetailById(stock.getId()).orElse(stock));
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> listTransactions(
            Long requestedCompanyId,
            Long requestedBranchId,
            Long warehouseId,
            Long sparePartId,
            String transactionType,
            Pageable pageable) {
        AppUser user = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return Page.empty(pageable);
        }
        Long branchFilter = listBranchFilter(user, requestedBranchId);
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Long> ids = transactionRepository.searchIds(
                companyId, warehouseId, sparePartId, branchFilter, normalizeFilter(transactionType), sorted);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), sorted, ids.getTotalElements());
        }
        Map<Long, InventoryTransaction> byId = transactionRepository.findDetailsByIds(ids.getContent()).stream()
                .collect(Collectors.toMap(InventoryTransaction::getId, Function.identity(), (a, b) -> a));
        List<InventoryTransactionResponse> content = ids.getContent().stream()
                .map(byId::get)
                .filter(row -> row != null)
                .map(this::toTransactionResponse)
                .toList();
        return new PageImpl<>(content, sorted, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public InventoryTransactionResponse getTransaction(Long id) {
        AppUser user = tenantAccess.requireCurrentUser();
        InventoryTransaction row = transactionRepository.findDetailById(id)
                .orElseThrow(() -> invalid("STOCK_NOT_FOUND", "Inventory transaction was not found."));
        assertReadable(row.getCompanyId(), row.getBranchId(), user);
        return toTransactionResponse(row);
    }

    private WarehouseStock addQuantity(Warehouse warehouse, SparePart part, BigDecimal quantity, String username) {
        Optional<WarehouseStock> locked = stockRepository.findActiveForUpdate(warehouse.getId(), part.getId());
        if (locked.isPresent()) {
            return increment(locked.get(), quantity, username);
        }
        WarehouseStock created = newBalance(warehouse, part, quantity, username);
        try {
            return stockRepository.saveAndFlush(created);
        } catch (DataIntegrityViolationException ex) {
            if (!isDuplicateStock(ex)) {
                throw ex;
            }
            WarehouseStock winner = stockRepository.findActiveForUpdate(warehouse.getId(), part.getId())
                    .orElseThrow(() -> invalid("STOCK_NOT_FOUND", "Stock balance was not found."));
            return increment(winner, quantity, username);
        }
    }

    private WarehouseStock increment(WarehouseStock stock, BigDecimal quantity, String username) {
        stock.setAvailableQuantity(stock.getAvailableQuantity().add(quantity));
        stock.setUpdatedBy(username);
        return stockRepository.saveAndFlush(stock);
    }

    private WarehouseStock newBalance(Warehouse warehouse, SparePart part, BigDecimal quantity, String username) {
        WarehouseStock stock = new WarehouseStock();
        stock.setCompanyId(warehouse.getCompanyId());
        stock.setBranchId(warehouse.getBranchId());
        stock.setWarehouse(warehouse);
        stock.setSparePart(part);
        stock.setAvailableQuantity(quantity);
        String code = (warehouse.getCode() + "-" + part.getCode());
        if (code.length() > 50) {
            code = code.substring(0, 50);
        }
        stock.setCode(code);
        stock.setName(part.getName());
        stock.setStatus("ACTIVE");
        stock.setCreatedBy(username);
        stock.setUpdatedBy(username);
        stock.setIsDeleted(false);
        return stock;
    }

    private SparePart requireUsablePart(Long sparePartId, Long companyId) {
        SparePart part = sparePartRepository.findDetailById(sparePartId)
                .orElseThrow(() -> invalid("SPARE_PART_NOT_FOUND", "Spare part was not found."));
        if (Boolean.TRUE.equals(part.getIsDeleted())) {
            throw invalid("SPARE_PART_DELETED", "A deleted spare part cannot receive opening stock.");
        }
        if (part.getCompanyId() == null || !part.getCompanyId().equals(companyId)) {
            throw invalid("SPARE_PART_NOT_FOUND", "The spare part does not belong to this company.");
        }
        return part;
    }

    private BigDecimal requireOpeningQuantity(BigDecimal raw) {
        if (raw == null) {
            throw invalid("OPENING_BALANCE_QUANTITY_REQUIRED", "Opening quantity is required.");
        }
        if (raw.compareTo(BigDecimal.ZERO) == 0) {
            throw invalid("OPENING_BALANCE_QUANTITY_INVALID", "Opening quantity must be greater than zero.");
        }
        if (raw.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("OPENING_BALANCE_QUANTITY_INVALID", "Opening quantity cannot be negative.");
        }
        try {
            return raw.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw invalid("OPENING_BALANCE_QUANTITY_INVALID", "Opening quantity cannot be negative.");
        }
    }

    private void assertReadable(Long companyId, Long branchId, AppUser user) {
        tenantAccess.assertCompanyAccess(companyId);
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getBranchId() != null && branchId != null && !user.getBranchId().equals(branchId)) {
            throw new AccessDeniedException("Access denied: Warehouse belongs to another branch.");
        }
    }

    private Long listBranchFilter(AppUser user, Long requestedBranchId) {
        if (tenantAccess.isSuperAdmin(user)) {
            return requestedBranchId;
        }
        return user.getBranchId() != null ? user.getBranchId() : requestedBranchId;
    }

    private WarehouseStockResponse toStockResponse(WarehouseStock stock) {
        WarehouseStockResponse dto = new WarehouseStockResponse();
        dto.setId(stock.getId());
        dto.setCompanyId(stock.getCompanyId());
        dto.setBranchId(stock.getBranchId());
        dto.setAvailableQuantity(stock.getAvailableQuantity());
        dto.setVersion(stock.getVersion());
        Warehouse warehouse = stock.getWarehouse();
        if (warehouse != null) {
            dto.setWarehouseId(warehouse.getId());
            dto.setWarehouseCode(warehouse.getCode());
            dto.setWarehouseName(warehouse.getName());
            if (warehouse.getBranch() != null) {
                dto.setBranchCode(warehouse.getBranch().getCode());
                dto.setBranchName(warehouse.getBranch().getName());
            }
        }
        SparePart part = stock.getSparePart();
        if (part != null) {
            dto.setSparePartId(part.getId());
            dto.setSparePartCode(part.getCode());
            dto.setSparePartName(part.getName());
            if (part.getDefaultUom() != null) {
                dto.setUomCode(part.getDefaultUom().getCode());
                dto.setUomName(part.getDefaultUom().getName());
            }
        }
        return dto;
    }

    private InventoryTransactionResponse toTransactionResponse(InventoryTransaction row) {
        InventoryTransactionResponse dto = new InventoryTransactionResponse();
        dto.setId(row.getId());
        dto.setCode(row.getCode());
        dto.setCreatedDate(row.getCreatedDate());
        dto.setTransactionType(row.getTransactionType());
        dto.setCompanyId(row.getCompanyId());
        dto.setBranchId(row.getBranchId());
        dto.setQuantity(row.getQuantity());
        dto.setReferenceType(row.getReferenceType());
        dto.setReferenceId(row.getReferenceId());
        dto.setCreatedBy(row.getCreatedBy());
        dto.setDescription(row.getDescription());
        Warehouse warehouse = row.getWarehouse();
        if (warehouse != null) {
            dto.setWarehouseId(warehouse.getId());
            dto.setWarehouseCode(warehouse.getCode());
            dto.setWarehouseName(warehouse.getName());
            if (warehouse.getBranch() != null) {
                dto.setBranchCode(warehouse.getBranch().getCode());
                dto.setBranchName(warehouse.getBranch().getName());
            }
        }
        SparePart part = row.getSparePart();
        if (part != null) {
            dto.setSparePartId(part.getId());
            dto.setSparePartCode(part.getCode());
            dto.setSparePartName(part.getName());
        }
        return dto;
    }

    private boolean isDuplicateStock(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("uk_warehouse_stock_warehouse_part");
    }

    private String blankToEmpty(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim();
    }

    private String normalizeFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
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
