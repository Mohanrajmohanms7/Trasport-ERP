package com.transport.erp.service;

import com.transport.erp.model.Customer;
import com.transport.erp.repository.CustomerRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public Page<Customer> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return customerRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return customerRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Customer> getById(Long id) {
        return customerRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(c -> {
                    tenantAccess.assertOwned(c.getCompanyId());
                    return c;
                });
    }

    @Transactional
    public Customer create(Customer customer) {
        Long companyId = tenantAccess.resolveCompanyId(customer.getCompanyId());
        customer.setCompanyId(companyId);
        if (customer.getCode() == null || customer.getCode().isBlank()) {
            throw new IllegalArgumentException("Customer code is required");
        }
        if (customerRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, customer.getCode()).isPresent()) {
            throw new IllegalArgumentException(
                    "Customer code already exists in this company: " + customer.getCode());
        }
        customer.setIsDeleted(false);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        tenantAccess.assertOwned(customer.getCompanyId());

        Optional<Customer> existing = customerRepository.findByCompanyIdAndCodeAndIsDeletedFalse(
                customer.getCompanyId(), customerDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Customer code already exists: " + customerDetails.getCode());
        }

        customer.setCode(customerDetails.getCode());
        customer.setName(customerDetails.getName());
        customer.setDescription(customerDetails.getDescription());
        customer.setStatus(customerDetails.getStatus());
        customer.setEmail(customerDetails.getEmail());
        customer.setPhone(customerDetails.getPhone());
        customer.setAddress(customerDetails.getAddress());
        customer.setGstNumber(customerDetails.getGstNumber());
        customer.setCreditLimit(customerDetails.getCreditLimit());
        if (customerDetails.getBranchId() != null) {
            customer.setBranchId(customerDetails.getBranchId());
        }

        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        tenantAccess.assertOwned(customer.getCompanyId());
        customer.setIsDeleted(true);
        customerRepository.save(customer);
    }

    @Transactional
    public Customer toggleStatus(Long id) {
        Customer customer = customerRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        tenantAccess.assertOwned(customer.getCompanyId());
        customer.setStatus("ACTIVE".equals(customer.getStatus()) ? "INACTIVE" : "ACTIVE");
        return customerRepository.save(customer);
    }
}
