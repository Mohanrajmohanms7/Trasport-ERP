package com.transport.erp.service;

import com.transport.erp.model.Supplier;
import com.transport.erp.repository.SupplierRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SupplierService {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    /** List with an optional status filter (dropdowns ask for ACTIVE only); no status = supplier list as before. */
    public Page<Supplier> getAll(Long companyId, String search, String status, Pageable pageable) {
        if (status == null || status.isBlank()) return getAll(companyId, search, pageable);
        return supplierRepository.searchByStatus(companyId, search == null ? "" : search.trim(), status.trim().toUpperCase(), pageable);
    }

    public Page<Supplier> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return supplierRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return supplierRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Supplier> getById(Long id) {
        return supplierRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .map(s -> {
                    tenantAccess.assertOwned(s.getCompanyId());
                    return s;
                });
    }

    @Transactional
    public Supplier create(Supplier supplier) {
        Long companyId = tenantAccess.resolveCompanyId(supplier.getCompanyId());
        supplier.setCompanyId(companyId);
        if (supplierRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, supplier.getCode()).isPresent()) {
            throw new IllegalArgumentException("Supplier code already exists: " + supplier.getCode());
        }
        supplier.setIsDeleted(false);
        validate(supplier);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, Supplier supplierDetails) {
        Supplier supplier = supplierRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        tenantAccess.assertOwned(supplier.getCompanyId());

        Optional<Supplier> existing = supplierRepository.findByCompanyIdAndCodeAndIsDeletedFalse(
                supplier.getCompanyId(), supplierDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Supplier code already exists: " + supplierDetails.getCode());
        }

        supplier.setCode(supplierDetails.getCode());
        supplier.setName(supplierDetails.getName());
        supplier.setDescription(supplierDetails.getDescription());
        supplier.setStatus(supplierDetails.getStatus());
        supplier.setEmail(supplierDetails.getEmail());
        supplier.setPhone(supplierDetails.getPhone());
        supplier.setAddress(supplierDetails.getAddress());
        supplier.setGstNumber(supplierDetails.getGstNumber());
        supplier.setCreditDays(supplierDetails.getCreditDays());
        validate(supplier);
        if (supplierDetails.getBranchId() != null) {
            supplier.setBranchId(supplierDetails.getBranchId());
        }

        return supplierRepository.save(supplier);
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        tenantAccess.assertOwned(supplier.getCompanyId());
        // A supplier with bills, payments or stock receipts is history: deactivate instead of deleting.
        Integer used = jdbcTemplate.queryForObject(
                "SELECT (SELECT COUNT(*) FROM supplier_bills WHERE supplier_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM supplier_payments WHERE supplier_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM inventory_transactions WHERE supplier_id = ? AND is_deleted = false)"
                        + " + (SELECT COUNT(*) FROM work_orders WHERE supplier_id = ? AND is_deleted = false)",
                Integer.class, id, id, id, id);
        if (used != null && used > 0) {
            throw new com.transport.erp.exception.BusinessValidationException("Supplier In Use", "SUPPLIER_IN_USE",
                    supplier.getName() + " has bills, payments, stock receipts or work orders.",
                    "Set the supplier to Inactive instead of deleting it.");
        }
        supplier.setIsDeleted(true);
        supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier toggleStatus(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        tenantAccess.assertOwned(supplier.getCompanyId());
        supplier.setStatus("ACTIVE".equals(supplier.getStatus()) ? "INACTIVE" : "ACTIVE");
        return supplierRepository.save(supplier);
    }

    private static void validate(Supplier s) {
        if (s.getName() == null || s.getName().isBlank()) throw new IllegalArgumentException("Supplier name is required.");
        if (s.getCode() == null || s.getCode().isBlank()) throw new IllegalArgumentException("Supplier code is required.");
        if (s.getCreditDays() != null && (s.getCreditDays() < 0 || s.getCreditDays() > 365)) {
            throw new IllegalArgumentException("Credit days must be between 0 and 365.");
        }
        if (s.getGstNumber() != null && !s.getGstNumber().isBlank()) {
            String g = s.getGstNumber().trim().toUpperCase();
            if (!g.matches("\\d{2}[A-Z0-9]{13}")) throw new IllegalArgumentException("GSTIN must be 15 characters starting with the 2-digit state code.");
            s.setGstNumber(g);
        }
    }
}
