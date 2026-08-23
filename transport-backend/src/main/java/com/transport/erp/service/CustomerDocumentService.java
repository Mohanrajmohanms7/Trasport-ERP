package com.transport.erp.service;

import com.transport.erp.model.Customer;
import com.transport.erp.model.CustomerDocument;
import com.transport.erp.repository.CustomerDocumentRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerDocumentService {

    @Autowired
    private CustomerDocumentRepository docRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<CustomerDocument> getDocumentsByCustomer(Long customerId) {
        parentAccess.requireCustomer(customerId);
        return docRepository.findByCustomerIdAndIsDeletedFalse(customerId);
    }

    @Transactional
    public CustomerDocument addDocument(Long customerId, CustomerDocument doc) {
        Customer customer = parentAccess.requireCustomer(customerId);

        doc.setCustomer(customer);
        doc.setIsDeleted(false);
        doc.setCode(doc.getDocType() + "_" + doc.getDocNumber());
        doc.setName(doc.getDocType() + " Document");
        doc.setCompanyId(customer.getCompanyId());
        doc.setBranchId(customer.getBranchId());

        return docRepository.save(doc);
    }

    @Transactional
    public void deleteDocument(Long id) {
        CustomerDocument doc = docRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        tenantAccess.assertCompanyAccess(doc.getCompanyId());
        doc.setIsDeleted(true);
        docRepository.save(doc);
    }
}
