package com.transport.erp.service;

import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleDocument;
import com.transport.erp.repository.VehicleDocumentRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleDocumentService {

    @Autowired
    private VehicleDocumentRepository docRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<VehicleDocument> getDocumentsByVehicle(Long vehicleId) {
        parentAccess.requireVehicle(vehicleId);
        return docRepository.findByVehicleIdAndIsDeletedFalse(vehicleId);
    }

    @Transactional
    public VehicleDocument addDocument(Long vehicleId, VehicleDocument doc) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);

        doc.setVehicle(vehicle);
        doc.setIsDeleted(false);
        doc.setCode(doc.getDocType() + "_" + doc.getDocNumber());
        doc.setName(doc.getDocType() + " Document");
        doc.setCompanyId(vehicle.getCompanyId());
        doc.setBranchId(vehicle.getBranchId());

        return docRepository.save(doc);
    }

    @Transactional
    public void deleteDocument(Long id) {
        VehicleDocument doc = docRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        tenantAccess.assertCompanyAccess(doc.getCompanyId());
        doc.setIsDeleted(true);
        docRepository.save(doc);
    }
}
