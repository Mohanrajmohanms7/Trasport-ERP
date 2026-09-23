package com.transport.erp.service;

import com.transport.erp.dto.WarehouseRequest;
import com.transport.erp.dto.WarehouseResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Branch;
import com.transport.erp.model.Warehouse;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.WarehouseRepository;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    static final String UNIQUE_CODE = "uk_warehouses_company_branch_code";
    private static final Set<String> WRITE_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> list(
            Long requestedCompanyId,
            Long requestedBranchId,
            String status,
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
                Sort.by(Sort.Direction.ASC, "code"));
        Page<Long> ids = warehouseRepository.searchIds(
                companyId, branchFilter, normalizeFilter(status), blankToEmpty(code), sorted);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), sorted, ids.getTotalElements());
        }
        Map<Long, Warehouse> byId = warehouseRepository.findDetailsByIds(ids.getContent()).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity(), (a, b) -> a));
        List<WarehouseResponse> content = ids.getContent().stream()
                .map(byId::get)
                .filter(row -> row != null)
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(content, sorted, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public WarehouseResponse get(Long id) {
        return toResponse(requireReadable(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        if (request == null) {
            throw invalid("WAREHOUSE_CODE_REQUIRED", "Warehouse code and name are required.");
        }
        Long companyId = tenantAccess.resolveCompanyId(request.getCompanyId());
        if (companyId == null) {
            throw invalid("WAREHOUSE_COMPANY_INVALID", "A company is required for the warehouse.");
        }
        tenantAccess.assertCompanyAccess(companyId);

        Branch branch = requireBranch(resolveManagedBranchId(user, request.getBranchId()), companyId);
        String code = requireCode(request.getCode());
        String name = requireName(request.getName());
        String status = requireStatus(request.getStatus());

        Warehouse warehouse = new Warehouse();
        warehouse.setCompanyId(companyId);
        warehouse.setBranchId(branch.getId());
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setDescription(trimToNull(request.getDescription()));
        warehouse.setStatus(status);
        warehouse.setCreatedBy(username);
        warehouse.setUpdatedBy(username);
        warehouse.setIsDeleted(false);
        try {
            warehouse = warehouseRepository.saveAndFlush(warehouse);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateCode(ex)) {
                throw invalid("WAREHOUSE_CODE_DUPLICATE", "An active warehouse with this code already exists in the branch.");
            }
            throw ex;
        }
        auditService.log(username, "WAREHOUSE_CREATED", "warehouses", warehouse.getId(), null, code);
        return toResponse(reload(warehouse.getId()));
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        Warehouse warehouse = lockReadable(id, user);
        if (request == null) {
            throw invalid("WAREHOUSE_CODE_REQUIRED", "Warehouse code and name are required.");
        }
        if (request.getBranchId() != null && !request.getBranchId().equals(warehouse.getBranchId())) {
            throw invalid("WAREHOUSE_BRANCH_INVALID", "A warehouse cannot be moved to another branch.");
        }
        if (request.getCompanyId() != null && !request.getCompanyId().equals(warehouse.getCompanyId())) {
            throw invalid("WAREHOUSE_COMPANY_INVALID", "A warehouse cannot be moved to another company.");
        }
        warehouse.setCode(requireCode(request.getCode()));
        warehouse.setName(requireName(request.getName()));
        warehouse.setDescription(trimToNull(request.getDescription()));
        warehouse.setStatus(requireStatus(request.getStatus() != null ? request.getStatus() : warehouse.getStatus()));
        warehouse.setUpdatedBy(username);
        try {
            warehouseRepository.saveAndFlush(warehouse);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateCode(ex)) {
                throw invalid("WAREHOUSE_CODE_DUPLICATE", "An active warehouse with this code already exists in the branch.");
            }
            throw ex;
        }
        auditService.log(username, "WAREHOUSE_UPDATED", "warehouses", warehouse.getId(), null, warehouse.getCode());
        return toResponse(reload(warehouse.getId()));
    }

    @Transactional
    public void delete(Long id, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        Warehouse warehouse = lockReadable(id, user);
        if (warehouseRepository.countPositiveStock(warehouse.getId()) > 0) {
            throw invalid("WAREHOUSE_NOT_EDITABLE", "A warehouse with on-hand stock cannot be deleted.");
        }
        warehouse.setIsDeleted(true);
        warehouse.setUpdatedBy(username);
        warehouseRepository.save(warehouse);
        auditService.log(username, "WAREHOUSE_UPDATED", "warehouses", warehouse.getId(), null, "deleted " + warehouse.getCode());
    }

    Warehouse requireUsable(Long id, AppUser user) {
        Warehouse warehouse = warehouseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> invalid("WAREHOUSE_NOT_FOUND", "Warehouse was not found."));
        assertReadable(warehouse, user);
        if (!"ACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            throw invalid("WAREHOUSE_INACTIVE", "An active warehouse is required.");
        }
        return warehouse;
    }

    private Warehouse requireReadable(Long id) {
        AppUser user = tenantAccess.requireCurrentUser();
        Warehouse warehouse = warehouseRepository.findDetailById(id)
                .orElseThrow(() -> invalid("WAREHOUSE_NOT_FOUND", "Warehouse was not found."));
        assertReadable(warehouse, user);
        return warehouse;
    }

    private Warehouse lockReadable(Long id, AppUser user) {
        Warehouse warehouse = warehouseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> invalid("WAREHOUSE_NOT_FOUND", "Warehouse was not found."));
        assertReadable(warehouse, user);
        return warehouse;
    }

    private Warehouse reload(Long id) {
        return warehouseRepository.findDetailById(id)
                .orElseThrow(() -> invalid("WAREHOUSE_NOT_FOUND", "Warehouse was not found."));
    }

    private void assertReadable(Warehouse warehouse, AppUser user) {
        tenantAccess.assertCompanyAccess(warehouse.getCompanyId());
        assertBranch(warehouse.getBranchId(), user, "Access denied: Warehouse belongs to another branch.");
    }

    private Long resolveManagedBranchId(AppUser user, Long requestedBranchId) {
        if (tenantAccess.isSuperAdmin(user)) {
            if (requestedBranchId == null) {
                throw invalid("WAREHOUSE_BRANCH_INVALID", "A branch is required.");
            }
            return requestedBranchId;
        }
        if (user.getBranchId() != null) {
            if (requestedBranchId != null && !user.getBranchId().equals(requestedBranchId)) {
                throw new AccessDeniedException("Access denied: Warehouse belongs to another branch.");
            }
            return user.getBranchId();
        }
        if (requestedBranchId == null) {
            throw invalid("WAREHOUSE_BRANCH_INVALID", "A branch is required.");
        }
        return requestedBranchId;
    }

    private Long listBranchFilter(AppUser user, Long requestedBranchId) {
        if (tenantAccess.isSuperAdmin(user)) {
            return requestedBranchId;
        }
        return user.getBranchId() != null ? user.getBranchId() : requestedBranchId;
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

    private Branch requireBranch(Long branchId, Long companyId) {
        if (branchId == null) {
            throw invalid("WAREHOUSE_BRANCH_INVALID", "A branch is required.");
        }
        Branch branch = branchRepository.findById(branchId)
                .filter(row -> !Boolean.TRUE.equals(row.getIsDeleted()))
                .orElseThrow(() -> invalid("WAREHOUSE_BRANCH_INVALID", "The selected branch was not found."));
        if (branch.getCompanyId() == null || !branch.getCompanyId().equals(companyId)) {
            throw invalid("WAREHOUSE_BRANCH_INVALID", "The selected branch does not belong to this company.");
        }
        return branch;
    }

    private String requireCode(String raw) {
        String code = raw != null ? raw.trim().toUpperCase(Locale.ROOT) : "";
        if (code.isEmpty() || code.length() > 50) {
            throw invalid("WAREHOUSE_CODE_REQUIRED", "Warehouse code is required.");
        }
        return code;
    }

    private String requireName(String raw) {
        String name = raw != null ? raw.trim() : "";
        if (name.isEmpty() || name.length() > 150) {
            throw invalid("WAREHOUSE_NAME_REQUIRED", "Warehouse name is required.");
        }
        return name;
    }

    private String requireStatus(String raw) {
        String status = raw == null || raw.isBlank() ? "ACTIVE" : raw.trim().toUpperCase(Locale.ROOT);
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw invalid("WAREHOUSE_NOT_EDITABLE", "Warehouse status must be ACTIVE or INACTIVE.");
        }
        return status;
    }

    void assertWriteAccess(AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getRoles() == null || user.getRoles().stream().map(AppRole::getCode).noneMatch(WRITE_ROLES::contains)) {
            throw new AccessDeniedException("Access denied: warehouse updates require COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    WarehouseResponse toResponse(Warehouse warehouse) {
        WarehouseResponse dto = new WarehouseResponse();
        dto.setId(warehouse.getId());
        dto.setCode(warehouse.getCode());
        dto.setName(warehouse.getName());
        dto.setDescription(warehouse.getDescription());
        dto.setStatus(warehouse.getStatus());
        dto.setCompanyId(warehouse.getCompanyId());
        dto.setBranchId(warehouse.getBranchId());
        dto.setVersion(warehouse.getVersion());
        if (warehouse.getBranch() != null) {
            dto.setBranchCode(warehouse.getBranch().getCode());
            dto.setBranchName(warehouse.getBranch().getName());
        }
        return dto;
    }

    private boolean isDuplicateCode(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.contains(UNIQUE_CODE);
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
                "Invalid Warehouse",
                code,
                code + ": " + message,
                "Check the warehouse code, name, branch, and status.");
    }
}
