package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.DriverDocument;
import com.transport.erp.service.DriverDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/documents")
@CrossOrigin(origins = "*")
public class DriverDocumentController {

    @Autowired
    private DriverDocumentService docService;

    @GetMapping
    public ApiResponse<List<DriverDocument>> getDocuments(@PathVariable Long driverId) {
        List<DriverDocument> docs = docService.getDocumentsByDriver(driverId);
        return ApiResponse.success(docs, "Driver documents fetched successfully");
    }

    @PostMapping
    public ApiResponse<DriverDocument> addDocument(
            @PathVariable Long driverId,
            @RequestBody DriverDocument doc) {
        DriverDocument created = docService.addDocument(driverId, doc);
        return ApiResponse.success(created, "Driver document uploaded successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long driverId,
            @PathVariable Long id) {
        docService.deleteDocument(id);
        return ApiResponse.success(null, "Driver document deleted successfully");
    }
}
