package com.transport.erp.service;

import com.transport.erp.model.Material;
import com.transport.erp.repository.MaterialRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public Page<Material> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return materialRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return materialRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Material> getById(Long id) {
        return materialRepository.findById(id)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(m -> {
                    tenantAccess.assertOwned(m.getCompanyId());
                    return m;
                });
    }

    @Transactional
    public Material create(Material material) {
        Long companyId = tenantAccess.resolveCompanyId(material.getCompanyId());
        material.setCompanyId(companyId);
        if (materialRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, material.getCode()).isPresent()) {
            throw new IllegalArgumentException("Material code already exists: " + material.getCode());
        }
        material.setIsDeleted(false);
        return materialRepository.save(material);
    }

    @Transactional
    public Material update(Long id, Material materialDetails) {
        Material material = materialRepository.findById(id)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + id));
        tenantAccess.assertOwned(material.getCompanyId());

        Optional<Material> existing = materialRepository.findByCompanyIdAndCodeAndIsDeletedFalse(
                material.getCompanyId(), materialDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Material code already exists: " + materialDetails.getCode());
        }

        material.setCode(materialDetails.getCode());
        material.setName(materialDetails.getName());
        material.setDescription(materialDetails.getDescription());
        material.setStatus(materialDetails.getStatus());
        material.setCategory(materialDetails.getCategory());
        material.setUnit(materialDetails.getUnit());
        material.setDefaultRate(materialDetails.getDefaultRate());
        material.setDensity(materialDetails.getDensity());
        if (materialDetails.getBranchId() != null) {
            material.setBranchId(materialDetails.getBranchId());
        }

        return materialRepository.save(material);
    }

    @Transactional
    public void delete(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + id));
        tenantAccess.assertOwned(material.getCompanyId());
        material.setIsDeleted(true);
        materialRepository.save(material);
    }

    @Transactional
    public Material toggleStatus(Long id) {
        Material material = materialRepository.findById(id)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + id));
        tenantAccess.assertOwned(material.getCompanyId());
        material.setStatus("ACTIVE".equals(material.getStatus()) ? "INACTIVE" : "ACTIVE");
        return materialRepository.save(material);
    }
}
