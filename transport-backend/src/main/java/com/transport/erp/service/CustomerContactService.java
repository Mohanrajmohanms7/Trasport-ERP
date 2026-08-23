package com.transport.erp.service;

import com.transport.erp.model.Customer;
import com.transport.erp.model.CustomerContact;
import com.transport.erp.repository.CustomerContactRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerContactService {

    @Autowired
    private CustomerContactRepository contactRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<CustomerContact> getContactsByCustomer(Long customerId) {
        parentAccess.requireCustomer(customerId);
        return contactRepository.findByCustomerIdAndIsDeletedFalse(customerId);
    }

    @Transactional
    public CustomerContact addContact(Long customerId, CustomerContact contact) {
        Customer customer = parentAccess.requireCustomer(customerId);

        contact.setCustomer(customer);
        contact.setIsDeleted(false);
        contact.setCode("CONT_" + customerId + "_" + System.currentTimeMillis());
        contact.setName(contact.getContactName());
        contact.setCompanyId(customer.getCompanyId());
        contact.setBranchId(customer.getBranchId());

        return contactRepository.save(contact);
    }

    @Transactional
    public void deleteContact(Long id) {
        CustomerContact contact = contactRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + id));
        tenantAccess.assertCompanyAccess(contact.getCompanyId());
        contact.setIsDeleted(true);
        contactRepository.save(contact);
    }
}
