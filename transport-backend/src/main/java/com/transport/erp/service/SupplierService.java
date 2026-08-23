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

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private TenantAccessService tenantAccess;

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
}
