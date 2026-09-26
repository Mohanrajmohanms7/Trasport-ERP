package com.transport.erp.service;

import com.transport.erp.dto.SparePartRequest;
import com.transport.erp.dto.SparePartResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.UomMaster;
import com.transport.erp.repository.SparePartRepository;
import com.transport.erp.repository.UomMasterRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SparePartService {

    private static final String UNIQUE_CODE = "uk_spare_parts_company_code";
    private static final Set<String> WRITE_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");

    @Autowired
    private SparePartRepository sparePartRepository;

    @Autowired
    private UomMasterRepository uomMasterRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    @Transactional(readOnly = true)
    public Page<SparePartResponse> list(Long requestedCompanyId, Pageable pageable) {
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return Page.empty(pageable);
        }
        return sparePartRepository.findActiveByCompany(companyId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UomMaster> usableUoms(Long requestedCompanyId) {
        Long companyId = tenantAccess.resolveCompanyId(requestedCompanyId);
        if (companyId == null) {
            return List.of();
        }
        return uomMasterRepository.findActiveForCompanyOrGlobal(companyId);
    }

    @Transactional
    public SparePartResponse create(SparePartRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        if (request == null) {
            throw invalid("Spare part code and name are required.");
        }
        Long companyId = tenantAccess.resolveCompanyId(request.getCompanyId());
        if (companyId == null) {
            throw invalid("A company is required for the spare part.");
        }
        tenantAccess.assertCompanyAccess(companyId);

        String code = request.getCode() != null ? request.getCode().trim().toUpperCase(Locale.ROOT) : "";
        String name = request.getName() != null ? request.getName().trim() : "";
        if (code.isEmpty() || code.length() > 50 || name.isEmpty() || name.length() > 150) {
            throw invalid("Spare part code and name are required.");
        }
        BigDecimal rate = request.getDefaultRate() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY) : money(request.getDefaultRate());
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("Default rate cannot be negative.");
        }
        String status = request.getStatus() == null || request.getStatus().isBlank()
                ? "ACTIVE"
                : request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw invalid("Spare part status must be ACTIVE or INACTIVE.");
        }

        UomMaster uom = requireUom(request.getDefaultUomId(), companyId);
        SparePart part = new SparePart();
        part.setCompanyId(companyId);
        part.setBranchId(null);
        part.setCode(code);
        part.setName(name);
        part.setDescription(trimToNull(request.getDescription()));
        part.setStatus(status);
        part.setDefaultUom(uom);
        part.setDefaultRate(rate);
        part.setReorderLevel(reorderLevel(request.getReorderLevel()));
        part.setCreatedBy(username);
        part.setUpdatedBy(username);
        part.setIsDeleted(false);
        try {
            part = sparePartRepository.saveAndFlush(part);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateCode(ex)) {
                throw invalid("An active spare part with this code already exists.");
            }
            throw ex;
        }
        auditService.log(username, "SPARE_PART_CREATED", "spare_parts", part.getId(), null, code);
        return toResponse(part);
    }

    /** Edit name, description, unit, default rate, reorder level and status. The code stays fixed (used on documents). */
    @Transactional
    public SparePartResponse update(Long id, SparePartRequest request, String username) {
        AppUser user = tenantAccess.requireCurrentUser();
        assertWriteAccess(user);
        SparePart part = sparePartRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> invalid("Spare part not found."));
        tenantAccess.assertCompanyAccess(part.getCompanyId());
        if (request == null) throw invalid("Spare part name is required.");
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty() || name.length() > 150) throw invalid("Spare part name is required.");
        BigDecimal rate = request.getDefaultRate() == null ? part.getDefaultRate() : money(request.getDefaultRate());
        if (rate != null && rate.compareTo(BigDecimal.ZERO) < 0) throw invalid("Default rate cannot be negative.");
        String status = request.getStatus() == null || request.getStatus().isBlank()
                ? part.getStatus() : request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) throw invalid("Spare part status must be ACTIVE or INACTIVE.");
        if (request.getDefaultUomId() != null) part.setDefaultUom(requireUom(request.getDefaultUomId(), part.getCompanyId()));
        part.setName(name);
        part.setDescription(trimToNull(request.getDescription()));
        part.setDefaultRate(rate);
        part.setReorderLevel(reorderLevel(request.getReorderLevel()));
        part.setStatus(status);
        part.setUpdatedBy(username);
        part = sparePartRepository.saveAndFlush(part);
        auditService.log(username, "SPARE_PART_UPDATED", "spare_parts", part.getId(), null, part.getCode());
        return toResponse(part);
    }

    private BigDecimal reorderLevel(BigDecimal v) {
        if (v == null) return null;
        if (v.compareTo(BigDecimal.ZERO) < 0) throw invalid("Reorder level cannot be negative.");
        return v.setScale(3, RoundingMode.HALF_UP);
    }

    private UomMaster requireUom(Long uomId, Long companyId) {
        if (uomId == null) {
            throw invalid("An active unit of measure is required.");
        }
        UomMaster uom = uomMasterRepository.findById(uomId)
                .filter(row -> !Boolean.TRUE.equals(row.getIsDeleted()))
                .orElseThrow(() -> invalid("An active unit of measure is required."));
        if (uom.getStatus() == null || !"ACTIVE".equalsIgnoreCase(uom.getStatus())) {
            throw invalid("An active unit of measure is required.");
        }
        if (uom.getCompanyId() != null && !uom.getCompanyId().equals(companyId)) {
            throw invalid("The unit of measure does not belong to this company.");
        }
        return uom;
    }

    private BigDecimal money(BigDecimal value) {
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private boolean isDuplicateCode(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.contains(UNIQUE_CODE);
    }

    private void assertWriteAccess(AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getRoles() == null || user.getRoles().stream().map(AppRole::getCode).noneMatch(WRITE_ROLES::contains)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: spare part updates require COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    private SparePartResponse toResponse(SparePart part) {
        SparePartResponse dto = new SparePartResponse();
        dto.setId(part.getId());
        dto.setCode(part.getCode());
        dto.setName(part.getName());
        dto.setDescription(part.getDescription());
        dto.setStatus(part.getStatus());
        dto.setCompanyId(part.getCompanyId());
        dto.setDefaultRate(part.getDefaultRate());
        dto.setVersion(part.getVersion());
        if (part.getDefaultUom() != null) {
            dto.setDefaultUomId(part.getDefaultUom().getId());
        dto.setPhotoFile(part.getPhotoFile());
        dto.setReorderLevel(part.getReorderLevel());
            dto.setDefaultUomCode(part.getDefaultUom().getCode());
            dto.setDefaultUomName(part.getDefaultUom().getName());
        }
        return dto;
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BusinessValidationException invalid(String message) {
        return new BusinessValidationException(
                "Invalid Spare Part",
                "SPARE_PART_INVALID",
                "SPARE_PART_INVALID: " + message,
                "Check the spare part code, name, unit, and rate.");
    }
}
