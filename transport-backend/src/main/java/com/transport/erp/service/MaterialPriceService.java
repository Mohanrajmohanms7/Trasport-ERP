package com.transport.erp.service;

import com.transport.erp.model.Material;
import com.transport.erp.model.MaterialPrice;
import com.transport.erp.repository.MaterialPriceRepository;
import com.transport.erp.repository.MaterialRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterialPriceService {

    @Autowired
    private MaterialPriceRepository priceRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<MaterialPrice> getPricesByMaterial(Long materialId) {
        Material material = requireMaterialInTenant(materialId);
        return priceRepository.findByMaterialIdAndIsDeletedFalse(material.getId());
    }

    @Transactional
    public MaterialPrice create(Long materialId, MaterialPrice price) {
        Material material = requireMaterialInTenant(materialId);

        price.setMaterial(material);
        price.setIsDeleted(false);
        price.setCode("PRICE_" + materialId + "_" + System.currentTimeMillis());
        price.setName("Material Pricing Entry");
        price.setCompanyId(material.getCompanyId());
        price.setBranchId(material.getBranchId());

        return priceRepository.save(price);
    }

    @Transactional
    public void delete(Long id) {
        MaterialPrice price = priceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing entry not found: " + id));
        tenantAccess.assertCompanyAccess(price.getCompanyId());
        price.setIsDeleted(true);
        priceRepository.save(price);
    }

    private Material requireMaterialInTenant(Long materialId) {
        Material material = materialRepository.findById(materialId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        tenantAccess.assertCompanyAccess(material.getCompanyId());
        return material;
    }
}
