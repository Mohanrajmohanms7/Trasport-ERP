package com.transport.erp.service;

import com.transport.erp.model.Customer;
import com.transport.erp.model.CustomerDeliverySite;
import com.transport.erp.repository.CustomerDeliverySiteRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerDeliverySiteService {

    @Autowired
    private CustomerDeliverySiteRepository siteRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<CustomerDeliverySite> getSitesByCustomer(Long customerId) {
        parentAccess.requireCustomer(customerId);
        return siteRepository.findByCustomerIdAndIsDeletedFalse(customerId);
    }

    @Transactional
    public CustomerDeliverySite addSite(Long customerId, CustomerDeliverySite site) {
        Customer customer = parentAccess.requireCustomer(customerId);

        site.setCustomer(customer);
        site.setIsDeleted(false);
        site.setCode(site.getSiteCode());
        site.setName(site.getSiteName());
        site.setCompanyId(customer.getCompanyId());
        site.setBranchId(customer.getBranchId());

        return siteRepository.save(site);
    }

    @Transactional
    public void deleteSite(Long id) {
        CustomerDeliverySite site = siteRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Delivery Site not found: " + id));
        tenantAccess.assertCompanyAccess(site.getCompanyId());
        site.setIsDeleted(true);
        siteRepository.save(site);
    }
}
