package com.transport.erp.service;

import com.transport.erp.model.Driver;
import com.transport.erp.model.DriverDocument;
import com.transport.erp.repository.DriverDocumentRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverDocumentService {

    @Autowired
    private DriverDocumentRepository docRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<DriverDocument> getDocumentsByDriver(Long driverId) {
        parentAccess.requireDriver(driverId);
        return docRepository.findByDriverIdAndIsDeletedFalse(driverId);
    }

    @Transactional
    public DriverDocument addDocument(Long driverId, DriverDocument doc) {
        Driver driver = parentAccess.requireDriver(driverId);

        doc.setDriver(driver);
        doc.setIsDeleted(false);
        doc.setCode(doc.getDocType() + "_" + doc.getDocNumber());
        doc.setName(doc.getDocType() + " Document");
        doc.setCompanyId(driver.getCompanyId());
        doc.setBranchId(driver.getBranchId());

        return docRepository.save(doc);
    }

    @Transactional
    public void deleteDocument(Long id) {
        DriverDocument doc = docRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        tenantAccess.assertCompanyAccess(doc.getCompanyId());
        doc.setIsDeleted(true);
        docRepository.save(doc);
    }
}
