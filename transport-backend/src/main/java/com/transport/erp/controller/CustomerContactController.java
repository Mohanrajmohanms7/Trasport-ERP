package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.CustomerContact;
import com.transport.erp.service.CustomerContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/contacts")
@CrossOrigin(origins = "*")
public class CustomerContactController {

    @Autowired
    private CustomerContactService contactService;

    @GetMapping
    public ApiResponse<List<CustomerContact>> getContacts(@PathVariable Long customerId) {
        List<CustomerContact> contacts = contactService.getContactsByCustomer(customerId);
        return ApiResponse.success(contacts, "Customer contacts fetched successfully");
    }

    @PostMapping
    public ApiResponse<CustomerContact> addContact(
            @PathVariable Long customerId,
            @RequestBody CustomerContact contact) {
        CustomerContact created = contactService.addContact(customerId, contact);
        return ApiResponse.success(created, "Customer contact created successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteContact(
            @PathVariable Long customerId,
            @PathVariable Long id) {
        contactService.deleteContact(id);
        return ApiResponse.success(null, "Customer contact deleted successfully");
    }
}
