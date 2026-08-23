package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Customer;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Customer>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Customer> customers = customerService.getAll(scopedCompanyId, search, pageable);
            return ApiResponse.success(customers, "Customers fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch customers");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> getById(@PathVariable Long id) {
        try {
            return customerService.getById(id)
                    .map(c -> ApiResponse.success(c, "Customer fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Customer not found"), "Customer not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch customer");
        }
    }

    @PostMapping
    public ApiResponse<Customer> create(@RequestBody Customer customer) {
        try {
            customer.setCompanyId(tenantAccess.resolveCompanyId(customer.getCompanyId()));
            Customer created = customerService.create(customer);
            return ApiResponse.success(created, "Customer created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create customer");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        try {
            Customer updated = customerService.update(id, customer);
            return ApiResponse.success(updated, "Customer updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update customer");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            customerService.delete(id);
            return ApiResponse.success(null, "Customer deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete customer");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Customer> toggleStatus(@PathVariable Long id) {
        try {
            Customer toggled = customerService.toggleStatus(id);
            return ApiResponse.success(toggled, "Customer status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
