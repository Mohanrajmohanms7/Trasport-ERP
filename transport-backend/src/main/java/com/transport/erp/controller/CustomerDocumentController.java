package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.CustomerDocument;
import com.transport.erp.service.CustomerDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/documents")
@CrossOrigin(origins = "*")
public class CustomerDocumentController {

    @Autowired
    private CustomerDocumentService docService;

    @GetMapping
    public ApiResponse<List<CustomerDocument>> getDocuments(@PathVariable Long customerId) {
        List<CustomerDocument> docs = docService.getDocumentsByCustomer(customerId);
        return ApiResponse.success(docs, "Customer documents fetched successfully");
    }

    @PostMapping
    public ApiResponse<CustomerDocument> addDocument(
            @PathVariable Long customerId,
            @RequestBody CustomerDocument doc) {
        CustomerDocument created = docService.addDocument(customerId, doc);
        return ApiResponse.success(created, "Customer document uploaded successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long customerId,
            @PathVariable Long id) {
        docService.deleteDocument(id);
        return ApiResponse.success(null, "Customer document deleted successfully");
    }
}
